package com.mojang.mojam.entity.building;

import java.awt.Color;
import java.util.Random;

import com.mojang.mojam.entity.Entity;
import com.mojang.mojam.entity.Player;
import com.mojang.mojam.entity.animation.SmokeAnimation;
import com.mojang.mojam.entity.loot.Loot;
import com.mojang.mojam.entity.loot.LootCollector;
import com.mojang.mojam.level.tile.Tile;
import com.mojang.mojam.network.TurnSynchronizer;
import com.mojang.mojam.screen.Art;
import com.mojang.mojam.screen.Bitmap;
import com.mojang.mojam.screen.Screen;

public class Harvester extends Building implements LootCollector {

	private int capacity = 1500;
	private int money = 0;
	private int time = 0;
	private int harvestingTicks = 20;
	private boolean isHarvesting = false;
	private boolean isEmptying = false;
	private Player emptyingPlayer = null;
	private int emptyingSpeed = 50;
	private int radius;
	private int[] upgradeRadius = new int[] { (int) (1.5 * Tile.WIDTH),
			2 * Tile.WIDTH, (int) (2.5 * Tile.WIDTH) };
	private int[] upgradeCapacities = new int[] { 1500, 2500, 3500 };

	public Harvester(double x, double y, int team, int localTeam) {
		super(x, y, team,localTeam);
		setStartHealth(10);
		freezeTime = 10;
		yOffs = 20;
		makeUpgradeableWithCosts(new int[] { 500, 1000, 5000 });
		healthBarOffset = 13;
		areaBitmap = Bitmap.rectangleBitmap(0,0,radius*2,radius*2,Color.YELLOW.getRGB());
	}
	
	public Bitmap areaBitmap;
	
	public void notifySucking() {
		harvestingTicks = 30;
	}

	public boolean isAllowedToCancel() {
		return !isEmptying;
	}

	public void tick() {
		super.tick();
		if (--freezeTime > 0) {
			return;
		}

		if (isEmptying && (time % 3 == 0)) {
			if (money <= 0) {
				isEmptying = false;
			} else {
				Loot l = new Loot(pos.x, pos.y, 0, 0, 1);
				l.fake = true;
				l.life = 20;
				l.forceTake(emptyingPlayer);
				int toAdd = Math.min(emptyingSpeed, money);
				money -= toAdd;
				emptyingPlayer.addScore(toAdd);
				level.addEntity(l);
			}
		}

		++time;
		--harvestingTicks;
		isHarvesting = (harvestingTicks >= 0);

		Random random = TurnSynchronizer.synchedRandom;
		if (isHarvesting) {
			if (random.nextDouble() < 0.050f) {
				level.addEntity(new SmokeAnimation(pos.x - 6
						+ random.nextInt(8) - random.nextInt(8), pos.y - 16,
						Art.fxSteam12, 30));
			}
		} else {
			if (random.nextDouble() < 0.002f) {
				level.addEntity(new SmokeAnimation(pos.x - 6
						+ random.nextInt(8) - random.nextInt(8), pos.y - 16,
						Art.fxSteam12, 30));
			}
		}
		if (health == 0) {
			dropAllMoney();
		}
	}

	@Override
	public Bitmap getSprite() {
		int frame = isHarvesting ? (4 + ((time >> 3) % 5)) : (time >> 3) % 4;
		switch (upgradeLevel) {
        case 1:
            return Art.harvester2[frame][0];
        case 2:
            return Art.harvester3[frame][0];
        default:
            return Art.harvester[frame][0];
        }
	}

	protected void upgradeComplete() {
	    maxHealth += 10;
	    health += 10;
        radius = upgradeRadius[upgradeLevel];
		capacity = upgradeCapacities[upgradeLevel];
		areaBitmap = Bitmap.rectangleBitmap(0,0,radius*2,radius*2,Color.YELLOW.getRGB());
		if (upgradeLevel != 0) justDroppedTicks = 80; //show the radius for a brief time
	}

	public boolean canTake() {
		return money < capacity;
	}

	public void render(Screen screen) {
		
		if(justDroppedTicks-- > 0 && localTeam==team) {
			screen.blit(areaBitmap, pos.x - areaBitmap.w / 2, pos.y - areaBitmap.h / 2 - yOffs);	
		}
		
		super.render(screen);

		Bitmap image = getSprite();
		if (capacity - money < 500) {
			screen.colorBlit(image, pos.x - image.w / 2, pos.y - image.h / 2 - yOffs, 0x77ff7200);
		}
		
		if(team ==localTeam && !isCarried()) {
			addMoneyBar(screen);
		}
		
	}
	
	private void addMoneyBar(Screen screen) {
        
	    int start = (int) (money * 20 / capacity);
        screen.blit(Art.moneyBar[start][0], pos.x - 16, pos.y + 8);
    }
	
	public void take(Loot loot) {
		loot.remove();
		money += loot.getScoreValue();
		if (money > capacity) {
			money = capacity;
		}
	}

	public double getSuckPower() {
		return radius / 60.0;
	}

	public void flash() {
	}

	public int getScore() {
		return money;
	}

	public void dropAllMoney() {

		while (money > 0) {
			double dir = TurnSynchronizer.synchedRandom.nextDouble() * Math.PI
					* 2;
			Loot loot = new Loot(pos.x, pos.y, Math.cos(dir), Math.sin(dir),
					money / 2);
			level.addEntity(loot);

			money -= loot.getScoreValue();
		}
		money = 0;
	}

	public void use(Entity user) {
		if(money > 0) {
			isEmptying = true;
			if (user instanceof Player) {
				emptyingPlayer = (Player) user;
			}
		} else {
			super.use(user);
		}
	}
}
