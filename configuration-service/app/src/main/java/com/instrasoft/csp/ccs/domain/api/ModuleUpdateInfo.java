package com.instrasoft.csp.ccs.domain.api;


public class ModuleUpdateInfo {

    private String name;
    private String description;
    private Integer version;
    private String released;
    private String hash;


    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getReleased() {
        return released;
    }

    public void setReleased(String released) {
        this.released = released;
    }

    public String getHash() {
        return hash;
    }

    public void setHash(String hash) {
        this.hash = hash;
    }


    @Override
    public String toString() {
        return "ModuleUpdateInfo{" +
                "name='" + name + '\'' +
                ", description='" + description + '\'' +
                ", version=" + version +
                ", released='" + released + '\'' +
                ", hash='" + hash + '\'' +
                '}';
    }
}
