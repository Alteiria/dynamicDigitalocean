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
import com.myjeeva.digitalocean.pojo.Region;
import com.myjeeva.digitalocean.pojo.Volume;
import com.myjeeva.digitalocean.pojo.Volumes;

import org.apache.commons.io.IOUtils;

public class DigitaloceanApi {

    private String apiKey;
    private String region;
    private String dropletSize;
    private int sshKeyID;
    private int imageID;
    private String domainName;;
    private String tag = "dynamicdigitalocean";
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
        if (getVolume(name) != null) {
            return (true);
        } else {
            return (false);
        }
    }

    public String getVolumeID(String name) {
        return (getVolume(name).getId());
    }

    public Volume getVolume(String name) {
        try {
            Volumes volumes = apiClient.getAvailableVolumes(region);
            for (int i = 0; i < volumes.getVolumes().size(); i++) {
                if (volumes.getVolumes().get(i).getName().equals(name)) {
                    return (volumes.getVolumes().get(i));
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
        try {
            Droplets droplets = apiClient.getAvailableDropletsByTagName(tag, 0, 30);
            for (int i = 0; i < droplets.getDroplets().size(); i++) {
                if (droplets.getDroplets().get(i).getName().equals(name)) {
                    return (droplets.getDroplets().get(i));
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
        if (!getDroplet(name).getNetworks().getVersion4Networks().isEmpty())
            return (getDroplet(name).getNetworks().getVersion4Networks().get(0).getIpAddress());
        return("0.0.0.0");
    }

    public DomainRecord getRecord(String name) {
        try {
            DomainRecords domainRecords = apiClient.getDomainRecords(domainName, 0, null);
            for (int i = 0; i < domainRecords.getDomainRecords().size(); i++) {
                if (domainRecords.getDomainRecords().get(i).getName().equals(name)) {
                    return (domainRecords.getDomainRecords().get(i));
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
        return (getRecord(name).getId());
    }

    public boolean hasRecord(String name) {
        if (getRecord(name) != null) {
            return (true);
        } else {
            return (false);
        }
    }

    public void addOrUpdateIPv4Record(String IPv4address, String name) {
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

        if (getDroplet(name) != null) {
            return (true);
        } else {
            return (false);
        }
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
                newDroplet.setTags(Arrays.asList(tag));
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