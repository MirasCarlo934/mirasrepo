package main;

import java.util.Iterator;
import java.util.List;

/**
 * The class that loads the filenames of the Spring Configuration files used by the BM.  
 * This is to make the configuration "dynamic".
 */
public class ConfigLoader {
    private List configFiles;
    private List engines;
    private List servers;


    /**
     * @return Returns the configFiles as an array of String.
     */
    public String[] getConfig() {
        String[] s = new String[configFiles.size()];
        int count = 0;
        for (Iterator i = configFiles.iterator(); i.hasNext(); ) {
            Object o = i.next();
            s[count] = (String) o;
            count++;
        }
        return s;
    }
    /**
     * @return Returns the configFiles.
     */
    public List getConfigFiles() {
        return configFiles;
    }
    /**
     * @param configFiles The configFiles to set.
     */
    public void setConfigFiles(List configFiles) {
        this.configFiles = configFiles;
    }
}
