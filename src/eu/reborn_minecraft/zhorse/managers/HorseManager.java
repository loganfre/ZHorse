package eu.reborn_minecraft.zhorse.managers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;

import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.Chunk;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.World;
import org.bukkit.attribute.Attribute;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.ChestedHorse;
import org.bukkit.entity.Entity;
import org.bukkit.entity.Horse;
import org.bukkit.entity.LeashHitch;
import org.bukkit.entity.Llama;
import org.bukkit.inventory.ItemStack;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import eu.reborn_minecraft.zhorse.ZHorse;
import eu.reborn_minecraft.zhorse.utils.DelayedChunckLoad;

public class HorseManager {
		
	private ZHorse zh;
	private Map<UUID, AbstractHorse> trackedHorses = new HashMap<>();
	
	public HorseManager(ZHorse zh) {
		this.zh = zh;
	}
	
	public AbstractHorse getFavoriteHorse(UUID playerUUID) {
		return getHorse(playerUUID, zh.getDM().getPlayerFavoriteHorseID(playerUUID));
	}
	
	public AbstractHorse getHorse(UUID playerUUID, Integer horseID) {
		AbstractHorse horse = null;
		if (playerUUID != null && horseID != null) {
			UUID horseUUID = zh.getDM().getHorseUUID(playerUUID, horseID);
			if (isHorseTracked(horseUUID)) {
				horse = getTrackedHorse(horseUUID);
			}
			else {
				Location location = zh.getDM().getHorseLocation(playerUUID, horseID);
				horse = getHorseFromLocation(horseUUID, location);
				if (horse != null && !horse.getLocation().equals(location)) {
					zh.getDM().updateHorseLocation(horseUUID, horse.getLocation(), false);
				}
			}
		}
		return horse;
	}
	
	private AbstractHorse getHorseFromLocation(UUID horseUUID, Location location) {
		AbstractHorse horse = null;
		if (location != null) {
			horse = getHorseInChunk(horseUUID, location.getChunk());
			if (horse == null) {
				List<Chunk> neighboringChunks = getChunksInRegion(location, 2, false);
				horse = getHorseInRegion(horseUUID, neighboringChunks);
			}
		}
		return horse;
	}
	
	private List<Chunk> getChunksInRegion(Location center, int chunkRange, boolean includeCentralChunk) {
		World world = center.getWorld();
		Location NWCorner = new Location(world, center.getX() - 16 * chunkRange, 0, center.getZ() - 16 * chunkRange);
		Location SECorner = new Location(world, center.getX() + 16 * chunkRange, 0, center.getZ() + 16 * chunkRange);
		List<Chunk> chunkList = new ArrayList<Chunk>();
		for (int x = NWCorner.getBlockX(); x <= SECorner.getBlockX(); x += 16) {
			for (int z = NWCorner.getBlockZ(); z <= SECorner.getBlockZ(); z += 16) {
				if (center.getBlockX() != x || center.getBlockZ() != z || includeCentralChunk) {
					Location chunkLocation = new Location(world, x, 0, z);
					Chunk chunk = world.getChunkAt(chunkLocation); // w.getChunkAt(x, z) uses chunk coordinates (loc % 16)
					chunkList.add(chunk);
				}
			}
		}
		return chunkList;
	}

	private AbstractHorse getHorseInChunk(UUID horseUUID, Chunk chunk) {
		for (Entity entity : chunk.getEntities()) {
			if (entity.getUniqueId().equals(horseUUID)) {
				return (AbstractHorse) entity;
			}
		}
		return null;
	}
	
	private AbstractHorse getHorseInRegion(UUID horseUUID, List<Chunk> region) {
		AbstractHorse horse = null;
		for (Chunk chunk : region) {
			horse = getHorseInChunk(horseUUID, chunk);
			if (horse != null) {
				return horse;
			}
		}
		return horse;
	}

	public AbstractHorse getTrackedHorse(UUID horseUUID) {
		return trackedHorses.get(horseUUID);
	}
	
	public Map<UUID, AbstractHorse> getTrackedHorses() {
		return trackedHorses;
	}
	
	public boolean isHorseTracked(UUID horseUUID) {
		return trackedHorses.containsKey(horseUUID);
	}
	
	public void trackHorse(AbstractHorse horse) {
		UUID horseUUID = horse.getUniqueId();
		if (!isHorseTracked(horseUUID)) {
			trackedHorses.put(horseUUID, horse);
		}
	}
	
	public void trackHorses() {
		for (World world : zh.getServer().getWorlds()) {
			for (Chunk chunk : world.getLoadedChunks()) {
				new DelayedChunckLoad(zh, chunk);
			}
		}
	}
	
	public void untrackHorse(AbstractHorse horse) {
		untrackHorse(horse.getUniqueId());
	}
	
	public void untrackHorse(UUID horseUUID) {
		if (isHorseTracked(horseUUID)) {
			trackedHorses.remove(horseUUID);
		}
	}
	
	public void untrackHorses() {
		Iterator<Entry<UUID, AbstractHorse>> trackedHorsesItr = trackedHorses.entrySet().iterator();
		while (trackedHorsesItr.hasNext()) {
			AbstractHorse horse = trackedHorsesItr.next().getValue();
			zh.getDM().updateHorseLocation(horse.getUniqueId(), horse.getLocation(), true);
			trackedHorsesItr.remove();
		}
	}
	
	public AbstractHorse teleport(AbstractHorse sourceHorse, Location destination) {
		if (zh.getCM().shouldUseOldTeleportMethod()) {
			sourceHorse.teleport(destination);
			zh.getDM().updateHorseLocation(sourceHorse.getUniqueId(), sourceHorse.getLocation(), false);
		}
		else {
			AbstractHorse copyHorse = (AbstractHorse) destination.getWorld().spawnEntity(destination, sourceHorse.getType());
			if (copyHorse != null) {
				String horseName = zh.getDM().getHorseName(sourceHorse.getUniqueId()); // call before updating horse uuid
				String ownerName = zh.getDM().getOwnerName(sourceHorse.getUniqueId());
				zh.getDM().updateHorseUUID(sourceHorse.getUniqueId(), copyHorse.getUniqueId());
				zh.getDM().updateHorseLocation(copyHorse.getUniqueId(), copyHorse.getLocation(), true);
				zh.getDM().updateHorseStatsUUID(sourceHorse.getUniqueId(), copyHorse.getUniqueId());
				copyAttributes(sourceHorse, copyHorse);
				copyInventory(sourceHorse, copyHorse);
				removeLeash(sourceHorse);
				untrackHorse(sourceHorse);
				trackHorse(copyHorse);
				removeHorse(sourceHorse, horseName, ownerName);
				return copyHorse;
			}
		}
		return null;
	}

	private void copyAttributes(AbstractHorse sourceHorse, AbstractHorse copyHorse) {	
		copyHorse.addPotionEffects(sourceHorse.getActivePotionEffects());
		copyHorse.setAge(sourceHorse.getAge());
		copyHorse.setBreed(sourceHorse.canBreed());
		copyHorse.setCanPickupItems(sourceHorse.getCanPickupItems());
		copyHorse.setCustomName(sourceHorse.getCustomName());
		copyHorse.setCustomNameVisible(sourceHorse.isCustomNameVisible());
		copyHorse.setDomestication(sourceHorse.getDomestication());
		copyHorse.setFireTicks(sourceHorse.getFireTicks());
		copyHorse.setGlowing(sourceHorse.isGlowing());
		double maxHealth = sourceHorse.getAttribute(Attribute.GENERIC_MAX_HEALTH).getBaseValue();
		copyHorse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(maxHealth);
		copyHorse.setHealth(sourceHorse.getHealth()); // Define max health before current health
		copyHorse.setJumpStrength(sourceHorse.getJumpStrength());
		copyHorse.setNoDamageTicks(sourceHorse.getNoDamageTicks());
		copyHorse.setOwner(sourceHorse.getOwner());
		copyHorse.setRemainingAir(sourceHorse.getRemainingAir());
		copyHorse.setTamed(sourceHorse.isTamed());
		copyHorse.setTicksLived(sourceHorse.getTicksLived());
		double speed = sourceHorse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
		copyHorse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
		
		switch (sourceHorse.getType()) {
		case HORSE:
			((Horse) copyHorse).setColor(((Horse) sourceHorse).getColor());
			((Horse) copyHorse).setStyle(((Horse) sourceHorse).getStyle());
			break;
		case LLAMA:
			((Llama) copyHorse).setColor(((Llama) sourceHorse).getColor());
			((Llama) copyHorse).setStrength(((Llama) sourceHorse).getStrength());
		default:
			break;
		}
	}
	
	private void copyInventory(AbstractHorse sourceHorse, AbstractHorse copyHorse) {
		if (sourceHorse instanceof ChestedHorse) {
			((ChestedHorse) copyHorse).setCarryingChest(((ChestedHorse) sourceHorse).isCarryingChest());
		}
		copyHorse.getInventory().setContents(sourceHorse.getInventory().getContents());
	}
	
	private void removeLeash(AbstractHorse horse) {
		if (horse.isLeashed()) {
			Entity leashHolder = horse.getLeashHolder();
			if (leashHolder instanceof LeashHitch) {
				leashHolder.remove();
			}
			horse.setLeashHolder(null);
			ItemStack leash = new ItemStack(Material.LEASH);
			horse.getWorld().dropItem(horse.getLocation(), leash);
		}
	}
	
	private void removeHorse(AbstractHorse horse, String horseName, String ownerName) {		
		Location horseLocation = horse.getLocation();
		UUID horseUUID = horse.getUniqueId();
		int waitTime = 60; // ticks
		horse.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, waitTime, 0));
		horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(0);
		horse.setAI(false);
		
		horse.remove();
		
		Bukkit.getScheduler().scheduleSyncDelayedTask(zh, new Runnable() {
			
			@Override
			public void run() {
				List<Chunk> neighboringChunks = getChunksInRegion(horseLocation, 1, true);
				AbstractHorse duplicatedHorse = getHorseInRegion(horseUUID, neighboringChunks);
				if (duplicatedHorse != null) {
					Location location = duplicatedHorse.getLocation();
					int x = location.getBlockX();
					int y = location.getBlockY();
					int z = location.getBlockZ();
					String warning = String.format("A horse named %s and owned by %s was duplicated at location %d:%d:%d in world %s, killing it.",
						horseName, ownerName, x, y, z, horseLocation.getWorld().getName());
					zh.getServer().broadcast(ChatColor.RED + warning, "zh.admin");
					zh.getLogger().severe(warning);
					duplicatedHorse.setHealth(0);
				}
			}
			
		}, waitTime);
	}

}
