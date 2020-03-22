package me.unixfox.dynamicDigitalocean;

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

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

import de.leonhard.storage.Toml;

public class DigitaloceanApi {

    private Toml toml = new Toml("config", "plugins/dynamicDigitalocean");
    private String apiKey = toml.get("general.apiKey", "yourapikey");
    private String region = toml.get("general.region", "ams3");
    private String dropletSize = toml.get("general.size", "s-2vcpu-4gb");
    private int sshKeyID = Integer.parseInt(toml.get("general.sshKeyID", "0"));
    private int imageID = Integer.parseInt(toml.get("general.imageID", "50944795"));
    private String domainName = toml.get("general.domain", "example.org");
    private String tag = "dynamicdigitalocean";
    private DigitalOcean apiClient = new DigitalOceanClient(apiKey);
    private String digitalOceanExceptionMessage = "DigitalOcean considered that the request was incorrect. Please verify that your config.toml is correct.";
    private String requestUnsuccessfulExceptionMessage = "Error communicating with DigitalOcean.";

    public DigitaloceanApi() {
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
            System.out.println(digitalOceanExceptionMessage);
            e.printStackTrace();
        } catch (RequestUnsuccessfulException e) {
            System.out.println(requestUnsuccessfulExceptionMessage);
            e.printStackTrace();
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
            System.out.println(digitalOceanExceptionMessage);
            e.printStackTrace();
        } catch (RequestUnsuccessfulException e) {
            System.out.println(requestUnsuccessfulExceptionMessage);
            e.printStackTrace();
        }
        return (null);
    }

    public String getDropletFirstIPv4(String name) {
        return (getDroplet(name).getNetworks().getVersion4Networks().get(0).getIpAddress());
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
            System.out.println(digitalOceanExceptionMessage);
            e.printStackTrace();
        } catch (RequestUnsuccessfulException e) {
            System.out.println(requestUnsuccessfulExceptionMessage);
            e.printStackTrace();
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
            System.out.println(digitalOceanExceptionMessage);
            e.printStackTrace();
        } catch (RequestUnsuccessfulException e) {
            System.out.println(requestUnsuccessfulExceptionMessage);
            e.printStackTrace();
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
                InputStream downloadUserData = new URL("https://github.com/Alteiria/dynamicDigitalocean/raw/master/vm/cloudinit.yaml").openStream();
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
                System.out.println(IPv4address);
            } else {
                System.out.println("The volume " + name + " doesn't exist yet, please create it manually.");
            }
        } catch (DigitalOceanException e) {
            System.out.println(digitalOceanExceptionMessage);
            e.printStackTrace();
        } catch (RequestUnsuccessfulException e) {
            System.out.println(requestUnsuccessfulExceptionMessage);
            e.printStackTrace();
        } catch (IOException e) {
            System.out.println("Error while downloading the user data.");
            e.printStackTrace();
        }
    }

    public void createRecordAndDeleteExisting(String name) {
        try {
            if (apiClient.getDomainInfo(domainName) != null) {
                DomainRecord input = new DomainRecord("test1", "@", "CNAME");
                apiClient.createDomainRecord("jeeutil.com", input);
            } else {
                System.out.println("Domain specified doesn't exist.");
            }
        } catch (DigitalOceanException e) {
            System.out.println(digitalOceanExceptionMessage);
            e.printStackTrace();
        } catch (RequestUnsuccessfulException e) {
            System.out.println(requestUnsuccessfulExceptionMessage);
            e.printStackTrace();
        }
    }
}