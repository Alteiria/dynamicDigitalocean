package me.unixfox.dynamicDigitalocean;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Arrays;
import java.util.logging.Level;

import com.myjeeva.digitalocean.DigitalOcean;
import com.myjeeva.digitalocean.exception.DigitalOceanException;
import com.myjeeva.digitalocean.exception.RequestUnsuccessfulException;
import com.myjeeva.digitalocean.impl.DigitalOceanClient;
import com.myjeeva.digitalocean.pojo.DomainRecord;
import com.myjeeva.digitalocean.pojo.DomainRecords;
import com.myjeeva.digitalocean.pojo.Droplet;
import com.myjeeva.digitalocean.pojo.Droplets;
import com.myjeeva.digitalocean.pojo.Image;
import com.myjeeva.digitalocean.pojo.Key;
import com.myjeeva.digitalocean.pojo.Network;
import com.myjeeva.digitalocean.pojo.Region;
import com.myjeeva.digitalocean.pojo.Volume;
import com.myjeeva.digitalocean.pojo.Volumes;

import org.apache.commons.io.IOUtils;

public class DigitaloceanApi {

    private static final String DROPLET_TAG = "dynamicdigitalocean";

    private String apiKey;
    private String region;
    private String dropletSize;
    private int sshKeyID;
    private int imageID;
    private String domainName;;
    private DigitalOcean apiClient;
    private String digitalOceanExceptionMessage = "DigitalOcean considered that the request was incorrect. Please verify that your config.toml is correct.";
    private String requestUnsuccessfulExceptionMessage = "Error communicating with DigitalOcean.";
    private final DynamicDigitalocean plugin;

    public DigitaloceanApi(final DynamicDigitalocean plugin) {
        this.plugin = plugin;
        this.apiKey = plugin.getDigitalOceanConfig().getString("general.apiKey");
        this.region = plugin.getDigitalOceanConfig().getString("general.region");
        this.dropletSize = plugin.getDigitalOceanConfig().getString("general.size");
        this.sshKeyID = plugin.getDigitalOceanConfig().getInt("general.sshKeyID");
        this.imageID = plugin.getDigitalOceanConfig().getInt("general.imageID");
        this.domainName = plugin.getDigitalOceanConfig().getString("general.domain");

        this.apiClient = new DigitalOceanClient(this.apiKey);
    }

    public boolean hasVolume(String name) {
        return getVolume(name) != null;
    }

    public String getVolumeID(String name) {
        Volume volume = getVolume(name);
        return (volume != null) ? volume.getId() : null;
    }

    public Volume getVolume(String name) {
        if (name == null) {
            return null;
        }
        try {
            Volumes volumes = apiClient.getAvailableVolumes(region);
            for (Volume volume : volumes.getVolumes()) {
                if (volume.getName().equals(name)) {
                    return volume;
                }
            }
        } catch (DigitalOceanException e) {
            plugin.getLogger().log(Level.WARNING, digitalOceanExceptionMessage, e);
        } catch (RequestUnsuccessfulException e) {
            plugin.getLogger().log(Level.WARNING, requestUnsuccessfulExceptionMessage, e);
        }
        return (null);
    }

    public Droplet getDroplet(String name) {
        if (name == null) {
            return null;
        }
        try {
            Droplets droplets = apiClient.getAvailableDropletsByTagName(DROPLET_TAG, 0, 30);
            for (Droplet droplet : droplets.getDroplets()) {
                if (droplet.getName().equals(name)) {
                    return droplet;
                }
            }
        } catch (DigitalOceanException e) {
            plugin.getLogger().log(Level.WARNING, digitalOceanExceptionMessage, e);
        } catch (RequestUnsuccessfulException e) {
            plugin.getLogger().log(Level.WARNING, requestUnsuccessfulExceptionMessage, e);
        }
        return (null);
    }

    public String getDropletFirstIPv4(String name) {
        Droplet droplet = getDroplet(name);
        if (droplet != null) {
            return droplet.getNetworks().getVersion4Networks().stream().findFirst().map(Network::getIpAddress).orElse(null);
        }
        return null;
    }

    public DomainRecord getRecord(String name) {
        if (name == null) {
            return null;
        }
        try {
            DomainRecords domainRecords = apiClient.getDomainRecords(domainName, null, null);
            for (DomainRecord record : domainRecords.getDomainRecords()) {
                if (record.getName().equals(name)) {
                    return record;
                }
            }
        } catch (DigitalOceanException e) {
            plugin.getLogger().log(Level.WARNING, digitalOceanExceptionMessage, e);
        } catch (RequestUnsuccessfulException e) {
            plugin.getLogger().log(Level.WARNING, requestUnsuccessfulExceptionMessage, e);
        }
        return (null);
    }

    public int getRecordID(String name) {
        DomainRecord record = getRecord(name);
        return (record != null) ? record.getId() : -1;
    }

    public boolean hasRecord(String name) {
        return getRecord(name) != null;
    }

    public void addOrUpdateIPv4Record(String IPv4address, String name) {
        if (IPv4address == null || name == null) return;
        try {
            if (apiClient.getDomainInfo(domainName) != null) {
                DomainRecord record = new DomainRecord(name, IPv4address, "A");
                if (hasRecord(name)) {
                    apiClient.updateDomainRecord(domainName, getRecordID(name), record);
                } else {
                    apiClient.createDomainRecord(domainName, record);
                }
            }
        } catch (DigitalOceanException e) {
            plugin.getLogger().log(Level.WARNING, digitalOceanExceptionMessage, e);
        } catch (RequestUnsuccessfulException e) {
            plugin.getLogger().log(Level.WARNING, requestUnsuccessfulExceptionMessage, e);
        }
    }

    public boolean hasDroplet(String name) {
        return getDroplet(name) != null;
    }

    public void createDroplet(String name) {
        String fqdn = name + "." + domainName;
        try {
            if (hasVolume(name)) {
                Droplet newDroplet = new Droplet();
                InputStream downloadUserData = new URL(
                        "https://github.com/Alteiria/dynamicDigitalocean/raw/master/vm/cloudinit.yaml").openStream();
                String userData = IOUtils.toString(downloadUserData);
                newDroplet.setName(fqdn);
                newDroplet.setSize(dropletSize);
                newDroplet.setRegion(new Region(region));
                newDroplet.setEnableIpv6(Boolean.TRUE);
                newDroplet.setImage(new Image(imageID));
                newDroplet.setVolumeIds(Arrays.asList(getVolumeID(name)));
                newDroplet.setUserData(userData);
                newDroplet.setTags(Arrays.asList(DROPLET_TAG));
                if (sshKeyID != 0)
                    newDroplet.setKeys(Arrays.asList(new Key(sshKeyID)));
                apiClient.createDroplet(newDroplet);
                String IPv4address = "0.0.0.0";
                while (getDroplet(fqdn).getNetworks().getVersion4Networks().isEmpty())
                    ;
                IPv4address = getDropletFirstIPv4(fqdn);
                addOrUpdateIPv4Record(IPv4address, name);
            } else {
                plugin.getLogger().warning("The volume " + name + " doesn't exist yet, please create it manually.");
            }
        } catch (DigitalOceanException e) {
            plugin.getLogger().log(Level.WARNING, digitalOceanExceptionMessage, e);
        } catch (RequestUnsuccessfulException e) {
            plugin.getLogger().log(Level.WARNING, requestUnsuccessfulExceptionMessage, e);
        } catch (IOException e) {
            plugin.getLogger().log(Level.WARNING, "Error while downloading the user data.", e);
        }
    }
}