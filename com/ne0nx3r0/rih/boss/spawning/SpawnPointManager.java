package com.ne0nx3r0.rih.boss.spawning;

import com.ne0nx3r0.rih.RareItemHunterPlugin;
import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.HashMap;
import java.util.Random;
import java.util.logging.Level;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.block.Block;
import org.bukkit.block.BlockFace;
import org.bukkit.configuration.ConfigurationSection;
import org.bukkit.configuration.file.YamlConfiguration;

public class SpawnPointManager {
    private final RareItemHunterPlugin plugin;
    private final HashMap<String, SpawnPoint> spawnPoints;

    public SpawnPointManager(RareItemHunterPlugin plugin) {
        this.plugin = plugin;
        
        this.spawnPoints = new HashMap<>(); 
        
        File ymlFile = new File(plugin.getDataFolder(),"spawnPoints.yml");

        YamlConfiguration config = YamlConfiguration.loadConfiguration(ymlFile);

        for(String spName : config.getKeys(false)){
            try{
                ConfigurationSection section = config.getConfigurationSection(spName);
                int x = section.getInt("x");
                int y = section.getInt("y");
                int z = section.getInt("z");
                int radius = section.getInt("radius");
                World world = plugin.getServer().getWorld(section.getString("world"));

                Location l = new Location(world,x,y,z);

                SpawnPoint sp = new SpawnPoint(spName,l,radius);

                this.spawnPoints.put(spName, sp);
            }
            catch(Exception ex){
                    plugin.getLogger().log(Level.WARNING, "Skipping spawn point {0} because:", spName);
                    
                    plugin.getLogger().log(Level.SEVERE, null, ex);
            }
        }
    }

    public Collection<SpawnPoint> getAllSpawnPoints() {
        return this.spawnPoints.values();
    }

    public SpawnPoint getSpawnPoint(String spName) {
        return this.spawnPoints.get(spName);
    }

    public boolean delSpawnPoint(String spName) {
        if(this.spawnPoints.remove(spName) != null){
            this.save();
            
            return true;
        }
        return false;
    }

    public void setSpawnPoint(String spName, Location spl, int radius) {
        this.spawnPoints.put(spName, new SpawnPoint(spName,spl,radius));
        
        this.save();
    }

    private void save() {
        this.plugin.getServer().getScheduler().runTaskAsynchronously(plugin, new Runnable(){
            @Override
            public void run() {
                File ymlFile = new File(plugin.getDataFolder(),"spawnPoints.yml");

                YamlConfiguration config = YamlConfiguration.loadConfiguration(ymlFile);
                
                for(SpawnPoint sp : spawnPoints.values()){
                    config.set(sp.getName()+".radius", sp.getRadius());
                    config.set(sp.getName()+".x", sp.getLocation().getBlockX());
                    config.set(sp.getName()+".y", sp.getLocation().getBlockY());
                    config.set(sp.getName()+".z", sp.getLocation().getBlockZ());
                    config.set(sp.getName()+".world", sp.getLocation().getWorld().getName());
                }
                
                try {
                    config.save(ymlFile);
                }
                catch (IOException ex) {
                    plugin.getLogger().warning("Unable to save spawn points!");
                    plugin.getLogger().log(Level.SEVERE, null, ex);
                }
            }
        });
    }

    public boolean isValidSpawnPoint(String sSpawnAt) {
        return this.spawnPoints.containsKey(sSpawnAt);
    }

    private final int MAX_SPAWN_TRIES = 25;
    
    public Location getRandomLocationAt(String sSpawnAt) {
        SpawnPoint sp = this.spawnPoints.get(sSpawnAt);
        
        if(sp == null){
            return null;
        }
        
        Location lCenter = sp.getLocation();
        Random r = new Random();
        
        int radius = sp.getRadius();
        
        Location lSpawnAt = null;
        World world = lCenter.getWorld();
        int maxHeight = world.getMaxHeight();
        
        for(int i=0;i<MAX_SPAWN_TRIES;i++){
            Location lRandom = lCenter.clone();
           
            // leave space beneath the egg to place bedrock
            lRandom.add(r.nextInt(radius*2)-radius, 2, r.nextInt(radius*2)-radius);
            
            lRandom.setY(0);
            
            //preload chunk
            world.getChunkAt(lRandom.getBlockX()/16, lRandom.getBlockZ()/16).load();
        
            while(lRandom.getBlockY() < maxHeight){
                Block b = lRandom.getBlock();
                
                if(b.getType().equals(Material.WATER)){
                    lSpawnAt = null;
                    
                    break;
                }
                else if(b.getType().equals(Material.AIR)
                && !b.getRelative(BlockFace.DOWN).getType().equals(Material.AIR)
                && b.getRelative(BlockFace.UP).getType().equals(Material.AIR)){
                    lSpawnAt = lRandom;
                    
                    break;
                }
                
                lRandom.add(0, 1, 0);
            }
            
            if(lSpawnAt != null){
                break;
            }
        }
        
        return lSpawnAt;
    }
}
