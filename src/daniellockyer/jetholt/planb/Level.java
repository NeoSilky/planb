package daniellockyer.jetholt.planb;

import java.util.ArrayList;
import java.util.List;

import org.newdawn.slick.*;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.geom.Vector2f;
import org.newdawn.slick.tiled.TiledMap;

import daniellockyer.jetholt.planb.entity.*;

public class Level {
	public static final int TILE_SIZE = 32;
	private TiledMap map;
	private Main main;
	private State layersToDraw = State.OUTSIDE;
	private ArrayList<Wall> walls = new ArrayList<Wall>();
	private ArrayList<Entity> entities = new ArrayList<Entity>();

	private final int OUTSIDE, FOYER, OFFICES, PREVAULT, VAULT, FLOOR;

	public Level(Main main, TiledMap map) {
		this.main = main;
		this.map = map;

		int objectGroupCount = map.getObjectGroupCount();
		for (int gi = 0; gi < objectGroupCount; gi++) {
			int objectCount = map.getObjectCount(gi);
			for (int oi = 0; oi < objectCount; oi++) {
				int x = map.getObjectX(gi, oi);
				int y = map.getObjectY(gi, oi);
				int width = map.getObjectWidth(gi, oi);
				int height = map.getObjectHeight(gi, oi);
				String a = map.getObjectName(gi, oi);

				Wall w = new Wall(new Rectangle(x, y, width, height));
				if (!a.equals("")) {
					w.setName(a);
					w.setWalkable(true);
				}

				walls.add(w);
			}
		}

		OUTSIDE = map.getLayerIndex("outside");
		FOYER = map.getLayerIndex("foyer");
		OFFICES = map.getLayerIndex("offices");
		PREVAULT = map.getLayerIndex("prevault");
		VAULT = map.getLayerIndex("vault");
		FLOOR = map.getLayerIndex("floor");

		for (Entity e : new ConfigFileReader().read()) {
			add(e);
		}

		/*
		 * add(new Civilian(155, 225)); add(new Civilian(355, 225)); add(new Civilian(555, 225));
		 */
	}

	public void update() {
		ArrayList<Entity> toremove = new ArrayList<Entity>();

		for (Entity e : entities) {
			e.update();
			if (e.removed) toremove.add(e);

		}
		entities.removeAll(toremove);
	}

	public void translate(int amount) {
		if (main.yOffset + (12 * amount) < -96 && main.yOffset + (12 * amount) > -372)
			main.yOffset += 12 * amount;
	}

	public void render(int yOffset, Graphics g) {
		map.render(0, yOffset, FLOOR);
		map.render(0, yOffset, VAULT);
		map.render(0, yOffset, PREVAULT);
		map.render(0, yOffset, OFFICES);
		map.render(0, yOffset, FOYER);

		for (String i : layersToDraw.layers()) {
			map.render(0, yOffset, map.getLayerIndex(i));
		}

		for (Entity e : entities)
			e.render(g);

		map.render(0, yOffset, OUTSIDE);

		if (true) {
			for (Wall w : walls) {
				g.setColor(Color.green);
				g.drawRect(w.getBoundaries().getX(), w.getBoundaries().getY() + yOffset, w
						.getBoundaries().getWidth(), w.getBoundaries().getHeight());
			}
		}

	}

	public void up() {
		if (layersToDraw == State.OUTSIDE) {
			layersToDraw = State.FOYER;
		} else if (layersToDraw == State.FOYER) {
			layersToDraw = State.OFFICES;
		} else if (layersToDraw == State.OFFICES) {
			layersToDraw = State.PREVAULT;
		} else if (layersToDraw == State.PREVAULT) {
			layersToDraw = State.VAULT;
		}
	}

	public void add(Entity e) {
		try {
			e.init(main, this);
			entities.add(e);
		} catch (SlickException e1) {
			e1.printStackTrace();
		}

	}

	public boolean wall(Vector2f pos, Entity e, float xa, float ya) {
		for (int i = 0; i < walls.size(); i++) {
			Wall w = walls.get(i);
			float x0 = w.getBoundaries().getX();
			float y0 = w.getBoundaries().getY() + main.yOffset;
			float x1 = pos.x + xa;
			float y1 = pos.y + ya;
			if (x1 + e.getWidth() >= x0 && x1 <= x0 + w.getWidth()) {
				if (y1 <= y0 + w.getHeight() && y1 + e.getHeight() >= y0) { return true; }
			}
		}
		return false;
	}

	public List<Entity> getEntities(Vector2f from, Entity ignore, int radius) {
		List<Entity> result = new ArrayList<Entity>();
		for (int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			if (e == ignore) continue;
			double dx = e.getPosition().x - from.x;
			double dy = e.getPosition().y - from.y;
			if (Math.sqrt(dx * dx + dy * dy) > radius) continue;
			result.add(e);
		}
		return result;
	}

	public List<Entity> getEntities(Entity from, int radius) {
		List<Entity> result = new ArrayList<Entity>();
		for (int i = 0; i < entities.size(); i++) {
			Entity e = entities.get(i);
			if (e == from) continue;
			double dx = e.getPosition().x - from.getPosition().x;
			double dy = e.getPosition().y - from.getPosition().y;
			if (Math.sqrt(dx * dx + dy * dy) > radius) continue;
			result.add(e);
		}
		return result;
	}

	public String wallName(Entity e) {
		for (int i = 0; i < walls.size(); i++) {
			Wall w = walls.get(i);
			float x0 = w.getBoundaries().getX();
			float y0 = w.getBoundaries().getY() + main.yOffset;
			float x1 = e.getPosition().x;
			float y1 = e.getPosition().y;
			if (x1 + e.getWidth() >= x0 && x1 <= x0 + w.getWidth() && y1 <= y0 + w.getHeight()
					&& y1 + e.getHeight() >= y0) { return w.getName(); }
		}
		return "";
	}

	public Wall getWallIntersect(Entity e) {
		for (int i = 0; i < walls.size(); i++) {
			Wall w = walls.get(i);
			float x0 = w.getBoundaries().getX();
			float y0 = w.getBoundaries().getY() + main.yOffset;
			float x1 = e.getPosition().x;
			float y1 = e.getPosition().y;
			if (x1 + e.getWidth() >= x0 && x1 <= x0 + w.getWidth() && y1 <= y0 + w.getHeight()
					&& y1 + e.getHeight() >= y0) { return w; }
		}
		return null;
	}

	public boolean wall(Entity e, float xa, float ya) {
		for (int i = 0; i < walls.size(); i++) {
			Wall w = walls.get(i);
			float x0 = w.getBoundaries().getX();
			float y0 = w.getBoundaries().getY() + main.yOffset;
			float x1 = e.getPosition().x + xa;
			float y1 = e.getPosition().y + ya;
			if (w.isWalkable()) return false;
			if (e instanceof Player) {
				if (x1 + e.getWidth() >= x0 && x1 <= x0 + w.getWidth()
						&& y1 <= (y0 + w.getHeight()) - w.getHeight() / 3
						&& y1 + e.getHeight() / 3 >= y0) { return true; }
			} else if (x1 + e.getWidth() >= x0 && x1 <= x0 + w.getWidth()
					&& y1 <= y0 + w.getHeight() && y1 + e.getHeight() >= y0) { return true; }
		}
		return false;
	}
}
