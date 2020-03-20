package me.unixfox.dynamicDigitalocean;

import com.myjeeva.digitalocean.DigitalOcean;
import com.myjeeva.digitalocean.exception.DigitalOceanException;
import com.myjeeva.digitalocean.exception.RequestUnsuccessfulException;
import com.myjeeva.digitalocean.impl.DigitalOceanClient;
import com.myjeeva.digitalocean.pojo.Droplets;
import com.myjeeva.digitalocean.pojo.Volumes;

/**
 * digitalocean
 */
public class DigitaloceanApi {

    private String apiKey;
    private DigitalOcean apiClient = new DigitalOceanClient(apiKey);

    public DigitaloceanApi(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean doesVolumeExist(String name, String region) throws DigitalOceanException, RequestUnsuccessfulException {
        Volumes volumes = apiClient.getAvailableVolumes(region);
        for (int i = 0; i < volumes.getVolumes().size(); i++) {
            if (volumes.getVolumes().get(i).getName() == name) {
                return (true);
            }
        }
        return (false);
    }

    public String getVolumeID(String name, String region) throws DigitalOceanException, RequestUnsuccessfulException {
        Volumes volumes = apiClient.getAvailableVolumes(region);
        for (int i = 0; i < volumes.getVolumes().size(); i++) {
            if (volumes.getVolumes().get(i).getName() == name) {
                return (volumes.getVolumes().get(i).getId());
            }
        }
        return("Volume doesn't exist");
    }

    public boolean doesDropletExist(String name, String region) throws DigitalOceanException, RequestUnsuccessfulException {
        Droplets droplets = apiClient.getAvailableDroplets(1, null);
        for (int i = 0; i < droplets.getDroplets().size(); i++) {
            if (droplets.getDroplets().get(i).getName() == name) {
                return (true);
            }
        }
        return(false);
    }
}