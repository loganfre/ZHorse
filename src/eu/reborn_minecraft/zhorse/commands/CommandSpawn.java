package eu.reborn_minecraft.zhorse.commands;

import java.util.Random;

import org.apache.commons.lang.StringUtils;
import org.bukkit.Location;
import org.bukkit.attribute.Attribute;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.AbstractHorse;
import org.bukkit.entity.Horse;
import org.bukkit.entity.Llama;

import eu.reborn_minecraft.zhorse.ZHorse;
import eu.reborn_minecraft.zhorse.enums.HorseStatisticEnum;
import eu.reborn_minecraft.zhorse.enums.HorseVariantEnum;
import eu.reborn_minecraft.zhorse.enums.LocaleEnum;

public class CommandSpawn extends AbstractCommand {
	
	private static final String DOUBLE_SEPARATOR = ":";
	
	private boolean valid = true;
	
	/* AbstractHorse attributes */
	private boolean baby = false;
	private boolean tamed = false;
	private double health = -1;
	private double jumpStrength = -1;
	private double speed = -1;
	private HorseVariantEnum variant = null;
	
	/* Horse attributes */
	private Horse.Color horseColor = null;
	private Horse.Style horseStyle = null;
	
	/* Llama attributes */
	private Llama.Color llamaColor = null;
	private int llamaStrength = -1;

	public CommandSpawn(ZHorse zh, CommandSender s, String[] a) {
		super(zh, s, a);
		playerOnly = true;
		needTarget = false;
		if (isPlayer() && analyseArguments() && hasPermission() && isWorldEnabled()) {
			if (!(idMode || targetMode)) {
				execute();
			}
			else {
				sendCommandUsage();
			}				
		}
	}

	private void execute() {
		if (zh.getEM().canAffordCommand(p, command)) {
			parseArguments();
			if (valid) {
				craftHorse();
				if (displayConsole) {
					zh.getMM().sendMessage(s, LocaleEnum.horseSpawned);
				}
				zh.getEM().payCommand(p, command);
			}
			else {
				sendCommandUsage();
			}
		}
	}

	private void parseArguments() {
		if (!argument.isEmpty()) {
			String[] argumentArray = argument.split(" "); // not using super.a to skip flags
			for (String argument : argumentArray) { // check for each token if it is some type of attribute
				boolean parsed = false;
				if (!parsed) {
					parsed = parseVariant(argument);
				}
				if (!parsed) {
					parsed = parseHorseStyle(argument);
				}
				if (!parsed) {
					parsed = parseLlamaColor(argument);
					parsed = parseHorseColor(argument);
				}
				if (!parsed) {
					parsed = parseTamed(argument);
				}
				if (!parsed) {
					parsed = parseBaby(argument);
				}
				if (!parsed) {
					parsed = parseDoubles(argument);
				}
				if (!parsed) {
					parsed = parseLlamaStrength(argument);
				}
				if (!parsed) {
					valid = false;
					zh.getMM().sendMessageValue(s, LocaleEnum.unknownSpawnArgument, argument);
				}
			}
		}
	}

	private boolean parseVariant(String argument) {
		for (HorseVariantEnum horseVariant : HorseVariantEnum.values()) {
			for (String horseVariantCode : horseVariant.getCodeList()) {
				if (argument.equalsIgnoreCase(horseVariantCode)) {
					if (variant == null) {
						variant = horseVariant;
						return true;
					}
					else {
						valid = false;
					}
				}
			}
		}
		return false;
	}
	
	private boolean parseHorseStyle(String argument) { // TODO merge with parseHorseColor
		for (Horse.Style existingStyle : Horse.Style.values()) {
			if (argument.equalsIgnoreCase(existingStyle.name())) {
				if (horseStyle == null) {
					horseStyle = existingStyle;
					return true;
				}
				else {
					boolean matchColor = false;
					for (Horse.Color existingColor : Horse.Color.values()) {
						if (argument.equalsIgnoreCase(existingColor.name())) {
							matchColor = true;
							break;
						}
					}
					if (!matchColor) {
						valid = false;
					}
				}
			}
		}
		return false;
	}
	
	private boolean parseHorseColor(String argument) { // TODO merge with parseHorseStyle
		for (Horse.Color existingColor : Horse.Color.values()) {
			if (argument.equalsIgnoreCase(existingColor.name())) {
				if (horseColor == null) {
					horseColor = existingColor;
					return true;
				}
				else {
					boolean matchStyle = false;
					for (Horse.Style existingStyle : Horse.Style.values()) {
						if (argument.equalsIgnoreCase(existingStyle.name())) {
							matchStyle = true;
							break;
						}
					}
					if (!matchStyle) {
						valid = false;
					}
				}
			}
		}
		return false;
	}
	
	private boolean parseLlamaColor(String argument) {
		for (Llama.Color existingColor : Llama.Color.values()) {
			if (argument.equalsIgnoreCase(existingColor.name())) {
				if (llamaColor == null) {
					llamaColor = existingColor;
					return true;
				}
				else {
					valid = false;
				}
			}
		}
		return false;
	}
	
	private boolean parseBaby(String argument) {
		if (argument.equalsIgnoreCase(HorseAttributeEnum.BABY.toString())) {
			if (!baby) {
				baby = true;
				return true;
			}
			else {
				valid = false;
			}
		}
		return false;
	}

	private boolean parseTamed(String argument) {
		if (argument.equalsIgnoreCase(HorseAttributeEnum.TAMED.toString())) {
			if (!tamed) {
				tamed = true;
				return true;
			}
			else {
				valid = false;
			}
		}
		return false;
	}
	
	private boolean parseDoubles(String argument) {
		if (StringUtils.countMatches(argument, DOUBLE_SEPARATOR) == 2) {
			if (health == -1 && jumpStrength == -1 && speed == -1) {
				Double[] doubles = buildDoubles(argument);
				if (doubles != null) {
					Double healthDouble = doubles[0];
					if (healthDouble != null) {
						valid &= isStatHealthValid(healthDouble);
						if (valid) {
							health = healthDouble;
						}
					}
					Double speedDouble = doubles[1];
					if (speedDouble != null) {
						valid &= isStatSpeedValid(speedDouble);
						if (valid) {
							double maxSpeed = HorseStatisticEnum.MAX_SPEED.getValue(useVanillaStats);
							speed = (speedDouble * maxSpeed) / 100;
						}
					}
					Double jumpDouble = doubles[2];
					if (jumpDouble != null) {
						valid &= isStatJumpStrengthValid(jumpDouble);
						if (valid) {
							double maxJumpStrength = HorseStatisticEnum.MAX_JUMP_STRENGTH.getValue(useVanillaStats);
							jumpStrength = (jumpDouble * maxJumpStrength) / 100;
						}
					}
					return true;
				}
				else {
					valid = false;
				}
			}
			else {
				valid = false;
			}
		}
		return false;
	}
	
	private Double[] buildDoubles(String argument) {
		int firstSeparatorIndex = argument.indexOf(DOUBLE_SEPARATOR);
		int secondSeparatorIndex = argument.indexOf(DOUBLE_SEPARATOR, firstSeparatorIndex + 1);
		String healthArg = argument.substring(0, firstSeparatorIndex);
		String speedArg = argument.substring(firstSeparatorIndex + 1, secondSeparatorIndex);
		String jumpArg = argument.substring(secondSeparatorIndex + 1);
		Double healthDouble = null;
		Double speedDouble = null;
		Double jumpDouble = null;
		try {
			if (!healthArg.isEmpty()) {
				healthDouble = Double.parseDouble(healthArg.replaceAll("%", ""));
			}
			if (!speedArg.isEmpty()) {
				speedDouble = Double.parseDouble(speedArg.replaceAll("%", ""));
			}
			if (!jumpArg.isEmpty()) {
				jumpDouble = Double.parseDouble(jumpArg.replaceAll("%", ""));
			}
		} catch (NumberFormatException e) {
			return null;
		}
		return new Double[] {healthDouble, speedDouble, jumpDouble};
	}
	
	private boolean parseLlamaStrength(String argument) {
		if (llamaStrength == -1) {
			int llamaStrengthInt;
			try {
				llamaStrengthInt = Integer.parseInt(argument);
			} catch (NumberFormatException e) {
				return false;
			}
			if (isStatLlamaStrengthValid(llamaStrengthInt)) {
				llamaStrength = llamaStrengthInt;
			}
			else {
				valid = false;
			}
		}
		return true;
	}

	private void craftHorse() {
		if (variant == null) {
			HorseVariantEnum[] variantArray = HorseVariantEnum.values();
			variant = variantArray[new Random().nextInt(variantArray.length)];
		}
		Location location = p.getLocation();
		AbstractHorse horse = (AbstractHorse) location.getWorld().spawnEntity(location, variant.getEntityType());
		
		checkAttributes(horse);
		
		horse.setOwner(p);
		horse.setRemoveWhenFarAway(false);
		horse.getAttribute(Attribute.GENERIC_MAX_HEALTH).setBaseValue(health);
		horse.setHealth(health);
		if (baby) {
			horse.setBaby();
		}
		else {
			horse.setAdult();
		}
		horse.setTamed(tamed);
		horse.setJumpStrength(jumpStrength);
		horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).setBaseValue(speed);
		switch (variant) {
		case HORSE:
			((Horse) horse).setStyle(horseStyle);
			((Horse) horse).setColor(horseColor);
			break;
		case LLAMA:
			((Llama) horse).setColor(llamaColor);
			((Llama) horse).setStrength(llamaStrength);
			break;
		default:
			break;
		}
	}
	
	private void checkAttributes(AbstractHorse horse) {
		if (health == -1) {
			health = horse.getHealth();
		}
		if (jumpStrength == -1) {
			jumpStrength = horse.getJumpStrength();
		}
		if (speed == -1) {
			speed = horse.getAttribute(Attribute.GENERIC_MOVEMENT_SPEED).getBaseValue();
		}
		switch (variant) {
		case HORSE:
			if (horseStyle == null) {
				horseStyle = ((Horse) horse).getStyle();
			}
			if (horseColor == null) {
				horseColor = ((Horse) horse).getColor();
			}
			break;
		case LLAMA:
			if (llamaColor == null) {
				llamaColor = ((Llama) horse).getColor();
			}
			if (llamaStrength == -1) {
				llamaStrength = ((Llama) horse).getStrength();
			}
			break;
		default:
			break;
		}
	}
	
	private enum HorseAttributeEnum {
		
		BABY, TAMED	
	
	}

}
