package daniellockyer.jetholt.planb;

import java.util.ArrayList;
import java.util.Collections;

import org.newdawn.slick.*;
import org.newdawn.slick.geom.Point;
import org.newdawn.slick.geom.Rectangle;
import org.newdawn.slick.tiled.TiledMap;
import org.newdawn.slick.util.pathfinding.PathFindingContext;
import org.newdawn.slick.util.pathfinding.TileBasedMap;

import daniellockyer.jetholt.planb.entity.*;

public class Level implements TileBasedMap {
	public static final int TILE_SIZE = 32;
	public TiledMap map;
	private Main main;
	private State layersToDraw = State.OUTSIDE;
	private ArrayList<Wall> walls = new ArrayList<Wall>();
	public ArrayList<Entity> entities = new ArrayList<Entity>();
	public ArrayList<Objective> objectiveList = new ArrayList<Objective>();
	private final int OUTSIDE, FOYER, OFFICES, PREVAULT, VAULT, FLOOR;

	public Level(Main main, TiledMap map) {
		this.main = main;
		this.map = map;

		int collisionsId = 0;

		for (int oi = 0; oi < map.getObjectCount(collisionsId); oi++) {
			int x = map.getObjectX(collisionsId, oi);
			int y = map.getObjectY(collisionsId, oi);
			int width = map.getObjectWidth(collisionsId, oi);
			int height = map.getObjectHeight(collisionsId, oi);
			String a = map.getObjectName(collisionsId, oi);

			Wall w = new Wall(new Rectangle(x, y, width, height));
			if (!a.equals("")) {
				w.setName(a);
				w.setWalkable(true);
			}

			walls.add(w);
		}

		int objectivesId = 1;

		for (int oi = 0; oi < map.getObjectCount(objectivesId); oi++) {
			int x = map.getObjectX(objectivesId, oi);
			int y = map.getObjectY(objectivesId, oi);
			int width = map.getObjectWidth(objectivesId, oi);
			int height = map.getObjectHeight(objectivesId, oi);

			String idString = map.getObjectName(objectivesId, oi);
			if (idString.isEmpty()) continue;

			int id = Integer.parseInt(idString.split("_")[1]);
			String message = map.getObjectProperty(objectivesId, oi, "message", null);
			int time = Integer.parseInt(map.getObjectProperty(objectivesId, oi, "time", null));
			objectiveList.add(new Objective(id, x, y, width, height, message, time));
		}
		Collections.sort(objectiveList);

		OUTSIDE = map.getLayerIndex("outside");
		FOYER = map.getLayerIndex("foyer");
		OFFICES = map.getLayerIndex("offices");
		PREVAULT = map.getLayerIndex("prevault");
		VAULT = map.getLayerIndex("vault");
		FLOOR = map.getLayerIndex("floor");

		for (Entity e : new ConfigFileReader().read()) {
			add(e);
		}

		main.gui.setMessage(objectiveList.get(0).getMessage());

		// add(new Civilian(155, 630));
		// add(new Cop(220, 850));
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
		if (main.yOffset + (12 * amount) < -96 && main.yOffset + (12 * amount) > -372) main.yOffset += 12 * amount;
	}

	public void render(Graphics g) {
		map.render(0, main.yOffset, FLOOR);
		map.render(0, main.yOffset, VAULT);
		map.render(0, main.yOffset, PREVAULT);
		map.render(0, main.yOffset, OFFICES);
		map.render(0, main.yOffset, FOYER);

		for (String i : layersToDraw.layers()) {
			map.render(0, main.yOffset, map.getLayerIndex(i));
		}

		for (Entity e : entities)
			e.render(g);

		map.render(0, main.yOffset, OUTSIDE);

		if (Main.DEBUG) {
			for (Wall w : walls) {
				g.setColor(Color.green);
				g.drawRect(w.getBoundaries().getX(), w.getBoundaries().getY() + main.yOffset, w.getBoundaries().getWidth(), w.getBoundaries().getHeight());
			}

			for (Objective o : objectiveList) {
				g.setColor(Color.magenta);
				g.drawRect(o.getX(), o.getY() + main.yOffset, o.getWidth(), o.getHeight());
			}
		}

	}

	public void up(Wall w) {
		if (layersToDraw == State.OUTSIDE) {
			layersToDraw = State.FOYER;
			w.done();
		} else if (layersToDraw == State.FOYER /*&& objectiveList.get(0).getID() == 2*/) {
			layersToDraw = State.OFFICES;
			w.done();
		} else if (layersToDraw == State.OFFICES /*&& objectiveList.get(0).getID() == 5*/) {
			layersToDraw = State.PREVAULT;
			w.done();
		} else if (layersToDraw == State.PREVAULT /*&& objectiveList.get(0).getID() == 7*/) {
			layersToDraw = State.VAULT;
			w.done();
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

	public Wall getWallIntersect(Entity e) {
		for (Wall w : walls) {
			Rectangle r1 = new Rectangle(w.getBoundaries().getX(), w.getBoundaries().getY() + main.yOffset, w.getWidth(), w.getHeight());
			Rectangle r2 = new Rectangle(e.getPosition().x, e.getPosition().y + e.getHeight() - (e.getHeight() / 6), e.getWidth(), e.getHeight() / 6);
			if (r1.intersects(r2)) return w;
		}
		return null;
	}

	public boolean wall(Entity e, float xa, float ya) {
		for (Wall w : walls) {
			Rectangle r = new Rectangle(w.getBoundaries().getX(), w.getBoundaries().getY() + main.yOffset, w.getWidth(), w.getHeight());

			if (e instanceof Player) {
				if (w.isWalkable()) continue;
				if (r.intersects(new Rectangle(e.getPosition().x + xa, e.getPosition().y + e.getHeight() - (e.getHeight() / 6) + ya, e.getWidth(), e.getHeight() / 6))) return true;
			} else if (e instanceof Bullet) {
				if (w.isWalkable() && w.been()) continue;
				if (r.intersects(new Rectangle(e.getPosition().x + xa, e.getPosition().y, e.getWidth(), e.getHeight()))) return true;
			}
		}
		return false;
	}

	@Override
	public int getWidthInTiles() {
		return Main.WIDTH / Level.TILE_SIZE;
	}

	@Override
	public int getHeightInTiles() {
		return Main.HEIGHT / Level.TILE_SIZE;
	}

	@Override
	public void pathFinderVisited(int x, int y) {
	}

	@Override
	public boolean blocked(PathFindingContext context, int tx, int ty) {
		for (Wall w : walls) {
			Rectangle r = new Rectangle(w.getBoundaries().getX(), w.getBoundaries().getY() + main.yOffset, w.getWidth(), w.getHeight());
			if (r.intersects(new Point(tx * Level.TILE_SIZE, ty * Level.TILE_SIZE))) { return true; }
		}
		return false;
	}

	@Override
	public float getCost(PathFindingContext context, int tx, int ty) {
		return 1.0f;
	}

}
