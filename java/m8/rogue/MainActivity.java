package m8.rogue;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Typeface;
import android.support.v7.app.ActionBarActivity;
import android.os.Bundle;
import android.os.Handler;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.ImageView;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class MainActivity extends ActionBarActivity {
    private static final char wall     = (char)  35;
    private static final char pass     = (char) 183;
    private static final char space    = (char)  32;
    private static final char door     = (char) 151;
    private static final char corpse   = (char)  37;
    private static final char chest    = (char) 172;
    private static final char clip     = (char)  61;
    private static final char waypoint = (char) 215;
    private static final char vendor   = (char) 165;
    private static final char stash    = (char) 177;
    private static final char stone    = (char)  94;
    private static final char gate     = (char)   8;
    private static final String saveFileName   = "rogue_state";
    private static final int maxX              =  39; // map width
    private static final int maxY              =  19; // map height
    private static final int maxDepth          =  90; // max level count
    private static final int warpDelay         = 300; // warp timer (turns before recharge)
    private static final int medpackCapacity   = 200; // medpack capacity
    private static final int medpackHealAmount = 100; // medpack heal amount
    private static final int inventoryCapacity =  13; // inventory capacity
    private static final int arMitigation      =  10; // armor mitigation value
    private static final int orbOfLifeValue    =  30; // orb of life value
    private static final int goldCacheValue    =  50; // gold cache value
    private static final int blindDuration     =   3; // flashbang duration
    private static final int paralyzeDuration  =   3; // paralyze duration
    private ImageView     imageView;
    private Canvas        canvas;
    private int           screenWidth;
    private int           screenHeight;
    private int           tileWidth;
    private int           tileHeight;
    private int           arrowWidth;
    private int           arrowHeight;
    private int           buttonWidth;
    private int           buttonHeight;
    private Paint         paint         = new Paint();
    private Random        random        = new Random();
    private List<Weapon>  gunTypes      = new ArrayList<Weapon>();
    private List<Weapon>  toolTypes     = new ArrayList<Weapon>();
    private List<Ammo>    ammoTypes     = new ArrayList<Ammo>();
    private List<Equip>   equipTypes    = new ArrayList<Equip>();
    private List<Monster> monsterTypes  = new ArrayList<Monster>();
    private List<Misc>    miscStuff     = new ArrayList<Misc>();
    private Tile[][]      map           = new Tile[maxY + 1][maxX + 1];
    private List<Room>    rooms         = new ArrayList<Room>();
    private List<Player>  monsters      = new ArrayList<Player>();
    private List<Player>  sorted        = new ArrayList<Player>();
    private List<Item>    items         = new ArrayList<Item>();
    private List<Item>    mine          = new ArrayList<Item>();
    private List<Item>    stashed       = new ArrayList<Item>();
    private String[]      messageArray  = new String[3];
    private Player        hero          = new Player();
    private int           depth         = 0;
    private int           medpackCharge = medpackCapacity;
    private int           warpCounter   = warpDelay;
    private int           fxDelay       = 25;
    private boolean       winCondition  = false;
    private char          gameState     = 'M';
    private int           progression   = 1;
    private int           selectedItem  = 0;
    private int           ptrPosition   = 0;
    private int           direction     = 0;
    private int           dX            = 0;
    private int           dY            = 0;
    private int           nextX         = 0;
    private int           nextY         = 0;
    private String        string        = "";

    private static class Tile implements Serializable {
        char    ch;
        boolean revealed;
        boolean highlighted;
    }

    private static class Room implements Serializable {
        int     x1, y1, x2, y2;
        boolean revealed = false;

        Room(int x1, int y1, int x2, int y2, boolean revealed) {
            this.x1 = x1; this.y1 = y1;
            this.x2 = x2; this.y2 = y2;
            this.revealed = revealed;
        }
    }

    private static class Weapon implements Serializable {
        String  name;
        int     damage;
        int     modifier;
        int     tier;

        Weapon(String name, int damage, int modifier, int tier) {
            this.name     = name;
            this.damage   = damage;
            this.modifier = modifier;
            this.tier     = tier;
        }
    }

    private class Ammo {
        String name;
        String weapon;
        int    quantity;
        int    price;

        Ammo(String name, String weapon, int quantity, int price) {
            this.name     = name;
            this.weapon   = weapon;
            this.quantity = quantity;
            this.price    = price;
        }
    }

    private static class Equip implements Serializable {
        String  name;
        String  type;
        int     armorValue;
        int     tier;

        Equip(String name, String type, int armorValue, int tier) {
            this.name       = name;
            this.type       = type;
            this.armorValue = armorValue;
            this.tier       = tier;
        }
    }

    private class Monster {
        char   face;
        String name;
        int    life;
        int    armor;
        String weapon;
        int    toughness;

        Monster(char face, String name, int life, int armor, String weapon, int toughness) {
            this.face      = face;
            this.name      = name;
            this.life      = life;
            this.armor     = armor;
            this.weapon    = weapon;
            this.toughness = toughness;
        }
    }

    private class Misc {
        String name;
        int    value;

        Misc(String name, int value) {
            this.name  = name;
            this.value = value;
        }
    }

    private static class Player implements Serializable {
        int     x, y;
        char    face;
        String  name;
        int     life;
        int     prevLife;
        int     armor;
        int     gold;
        Weapon  firearm;
        Weapon  tool;
        int     ammo;
        Equip   head;
        Equip   chest;
        Equip   glove;
        Equip   legs;
        Equip   ring1, ring2;
        boolean pursuit;
        int     blindTimer;
        int     paralyzeTimer;
        boolean state;
        char    prevCell;
    }

    private static class Item implements Serializable {
        int     x, y;
        String  name;
        int     amount;
        char    tag;
        int     price;
        boolean marked;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        imageView     = (ImageView) this.findViewById(R.id.imageView);
        Bitmap bitmap = Bitmap.createBitmap((int) getWindowManager()
                        .getDefaultDisplay().getWidth(), (int) getWindowManager()
                        .getDefaultDisplay().getHeight(), Bitmap.Config.ARGB_8888);
        canvas        = new Canvas(bitmap);
        screenWidth   = bitmap.getWidth();
        screenHeight  = bitmap.getHeight();
        tileWidth     = screenWidth / (maxX + 1);
        tileHeight    = screenHeight / ((maxY + 1) * 2);
        arrowWidth    = BitmapFactory.decodeResource(getResources(), R.drawable.home).getWidth();
        arrowHeight   = BitmapFactory.decodeResource(getResources(), R.drawable.home).getHeight();
        buttonWidth   = BitmapFactory.decodeResource(getResources(), R.drawable.button).getWidth();
        buttonHeight  = BitmapFactory.decodeResource(getResources(), R.drawable.button).getHeight();
        paint.setTypeface(Typeface.MONOSPACE);
        paint.setTextSize(tileHeight - (tileHeight / 8));
        paint.setFakeBoldText(true);
        paint.setAntiAlias(true);
        imageView.setImageBitmap(bitmap);
        imageView.setPadding(AR(0), AR(0), AR(0), AR(0));
        imageView.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (event.getAction() == MotionEvent.ACTION_DOWN)
                    if (hero.life > 0 && !winCondition) {
                        float xRatio = (float) screenWidth / imageView.getWidth();
                        float yRatio = (float) screenHeight / imageView.getHeight();
                        touchScreen(event.getX() * xRatio, event.getY() * yRatio);
                    }
                return true;
            }
        });
        paint.setColor(Color.DKGRAY);
        canvas.drawText("1.0.25", 0, screenHeight, paint);
        drawControls();
        initialize();
        File file = new File(MainActivity.this.getFilesDir().getAbsolutePath() + '/' +saveFileName);
        if (file.exists()) {
            readState();
            drawMap();
        } else {
            displayMessage("The guilty pay the price.");
            Item item   = new Item();
            item.x      = 100;
            item.y      = 100;
            item.name   = "homeward scroll";
            item.amount = 1;
            item.tag    = 'H';
            items.add(item);
            townInstance();
        }
        statusUpdate();
    }

    private int AR(int input) {
        final float scale = getResources().getDisplayMetrics().density;
        return (int) (input * scale + 0.5f);
    }

    private void touchScreen(float x, float y) {
        int mX = 0, mY = 0;

        for (int i = 0; i < messageArray.length; i++) messageArray[i] = "";
        switch (gameState) {
            case 'M' : // moving around
                clearMessages();
                if (arrowHomeHit(x, y)) { // home
                    direction = 7;
                }
                if (arrowUpHit(x, y)) { // up
                    direction = 8;
                }
                if (arrowPgUpHit(x, y)) { // pgup
                    direction = 9;
                }
                if (arrowLeftHit(x, y)) { // left
                    direction = 4;
                }
                if (arrowWaitHit(x, y)) { // wait
                    direction = 5;
                }
                if (arrowRightHit(x, y)) { // right
                    direction = 6;
                }
                if (arrowEndHit(x, y)) { // end
                    direction = 1;
                }
                if (arrowDownHit(x, y)) { // down
                    direction = 2;
                }
                if (arrowPgDnHit(x, y)) { // pgdn
                    direction = 3;
                }
                if (button1Hit(x, y) && hero.paralyzeTimer >= paralyzeDuration) { // heal
                    displayMessage("Use life flask?");
                    drawConfirmationButtons();
                    gameState = 'H';
                }
                if (button2Hit(x, y) && hero.paralyzeTimer >= paralyzeDuration) { // fire
                    if (hero.firearm.name.equals("empty")) { direction = 0; break; }
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.swtch),  screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    gameState = 'F';
                    for (Player m : monsters) if (m.state) sorted.add(m);
                    if (sorted.size() > 1)
                        for (int i = 0; i < sorted.size(); i++)
                            for (int j = 0; j < sorted.size() - i - 1; j++)
                                if (Math.abs(sorted.get(j).x     - hero.x) + Math.abs(sorted.get(j).y     - hero.y) >
                                    Math.abs(sorted.get(j + 1).x - hero.x) + Math.abs(sorted.get(j + 1).y - hero.y)) {
                                    Player p = sorted.get(j);
                                    sorted.set(j, sorted.get(j + 1));
                                    sorted.set(j + 1, p);
                                }
                    boolean locked = false;
                    if (sorted.size() > 0 && hero.blindTimer >= blindDuration)
                        for (Player m : sorted)
                            if (connectXY(m.x, m.y, hero.x, hero.y, 'c') && map[m.y][m.x].revealed) {
                                locked = true;
                                dX = m.x - hero.x;
                                dY = m.y - hero.y;
                                connectXY(hero.x, hero.y, hero.x + dX, hero.y + dY, 'h');
                                break;
                            }
                    if (!locked) map[hero.y][hero.x].highlighted = true;
                    displayMessage(String.format("Select a target to shoot. (%s)", tileInfo(hero.x + dX, hero.y + dY)));
                    drawMap();
                }
                if (button3Hit(x, y) && hero.paralyzeTimer >= paralyzeDuration) { // warp
                    displayMessage("Use warp crystal?");
                    drawConfirmationButtons();
                    gameState = 'W';
                }
                if (button4Hit(x, y) && hero.blindTimer >= blindDuration) { // inspect
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    gameState = 'L';
                    map[hero.y][hero.x].highlighted = true;
                    displayMessage(String.format("Select a target to examine. (%s)", tileInfo(hero.x, hero.y)));
                    drawMap();
                }
                if (button5Hit(x, y) && hero.blindTimer >= blindDuration) { // items
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.equip),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.info),   screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.use),    screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.drop),   screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    for (Item i : items) if (i.x == 0) mine.add(i);
                    gameState = 'I';
                    listItems("Inventory");
                }
                if (button6Hit(x, y) && hero.blindTimer >= blindDuration) { // equipment
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.info),   screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.remove), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    gameState = 'E';
                    listEquipment();
                }
                if (direction > 0 && direction < 10) {
                    if (hero.paralyzeTimer < paralyzeDuration) {
                        direction = 5;
                        hero.paralyzeTimer++;
                    }
                    if (hero.blindTimer < blindDuration) hero.blindTimer++;
                    if (warpCounter < warpDelay) {
                        warpCounter++;
                        if (hero.legs.name.equals("Boots of the Explorer")) warpCounter++;
                    }
                    movePlayer();
                    if (hero.ring1.name.equals("Blood Ring")) {
                        hero.prevLife = hero.life;
                        hero.life    -= 10;
                    }
                    if (hero.ring2.name.equals("Blood Ring")) {
                        hero.prevLife = hero.life;
                        hero.life    -= 10;
                    }
                }
                if (direction > 0 && direction < 10) { moveMonsters(); drawMap(); }
                direction = 0;
                break;
            case 'L' : // inspect
                if (arrowHomeHit(x, y)) { // home
                    mX--; mY--;
                }
                if (arrowUpHit(x, y)) { // up
                    mY--;
                }
                if (arrowPgUpHit(x, y)) { // pgup
                    mX++; mY--;
                }
                if (arrowLeftHit(x, y)) { // left
                    mX--;
                }
                if (arrowRightHit(x, y)) { // right
                    mX++;
                }
                if (arrowEndHit(x, y)) { // end
                    mX--; mY++;
                }
                if (arrowDownHit(x, y)) { // down
                    mY++;
                }
                if (arrowPgDnHit(x, y)) { // pgdn
                    mX++; mY++;
                }
                if (button4Hit(x, y)) { // confirm
                    inspectCell(hero.x + dX, hero.y + dY);
                    clearMapHighlights();
                    drawMap();
                    drawDefaultButtons();
                    dX = 0; dY = 0;
                    gameState = 'M';
                    direction = 0;
                    break;
                }
                if (button6Hit(x, y)) { // cancel
                    clearMapHighlights();
                    clearMessages();
                    drawMap();
                    drawDefaultButtons();
                    dX = 0; dY = 0;
                    gameState = 'M';
                    direction = 0;
                    break;
                }
                if (hero.x + dX + mX < maxX + 1 && hero.x + dX + mX > -1) dX += mX;
                if (hero.y + dY + mY < maxY + 1 && hero.y + dY + mY > -1) dY += mY;
                clearMapHighlights();
                clearMessages();
                displayMessage(String.format("Select a target to examine. (%s)", tileInfo(hero.x + dX, hero.y + dY)));
                map[hero.y + dY][hero.x + dX].highlighted = true;
                drawMap();
                break;
            case 'F' : // targeting
                if (arrowHomeHit(x, y)) { // home
                    mX--; mY--;
                }
                if (arrowUpHit(x, y)) { // up
                    mY--;
                }
                if (arrowPgUpHit(x, y)) { // pgup
                    mX++; mY--;
                }
                if (arrowLeftHit(x, y)) { // left
                    mX--;
                }
                if (arrowRightHit(x, y)) { // right
                    mX++;
                }
                if (arrowEndHit(x, y)) { // end
                    mX--; mY++;
                }
                if (arrowDownHit(x, y)) { // down
                    mY++;
                }
                if (arrowPgDnHit(x, y)) { // pgdn
                    mX++; mY++;
                }
                if (button2Hit(x, y)) { // fire
                    connectXY(hero.x, hero.y, hero.x + dX, hero.y + dY, 'a');
                    sorted.clear();
                    if (hero.blindTimer < blindDuration) hero.blindTimer++;
                    moveMonsters();
                    clearMapHighlights();
                    drawMap();
                    drawDefaultButtons();
                    dX = 0; dY = 0;
                    gameState = 'M';
                    break;
                }
                if (button4Hit(x, y) && hero.blindTimer >= blindDuration) { // switch
                    int j = 0, k = 0;
                    for (int i = 0; i < sorted.size(); i++)
                        if (sorted.get(i).x == hero.x + dX && sorted.get(i).y == hero.y + dY) {
                            j = i; break;
                        }
                    j = j == sorted.size() - 1 ? 0 : j + 1;
                    boolean locked = false;
                    for (int i = j; i < sorted.size(); i++)
                        if (connectXY(sorted.get(i).x, sorted.get(i).y, hero.x, hero.y, 'c') &&
                            map[sorted.get(i).y][sorted.get(i).x].revealed) {
                            k = i; locked = true;
                            break;
                        }
                    if (!locked)
                        for (int i = 0; i < sorted.size(); i++)
                            if (connectXY(sorted.get(i).x, sorted.get(i).y, hero.x, hero.y, 'c') &&
                                map[sorted.get(i).y][sorted.get(i).x].revealed) {
                                k = i; locked = true;
                                break;
                            }
                    if (locked) {
                        dX = sorted.get(k).x - hero.x;
                        dY = sorted.get(k).y - hero.y;
                    }
                }
                if (button6Hit(x, y)) { // cancel
                    sorted.clear();
                    clearMessages();
                    clearMapHighlights();
                    drawMap();
                    drawDefaultButtons();
                    dX = 0; dY = 0;
                    gameState = 'M';
                    direction = 0;
                    break;
                }
                if (hero.x + dX + mX < maxX + 1 && hero.x + dX + mX > -1) dX += mX;
                if (hero.y + dY + mY < maxY + 1 && hero.y + dY + mY > -1) dY += mY;
                clearMapHighlights();
                clearMessages();
                displayMessage(String.format("Select a target to shoot. (%s)", tileInfo(hero.x + dX, hero.y + dY)));
                connectXY(hero.x, hero.y, hero.x + dX, hero.y + dY, 'h');
                drawMap();
                break;
            case 'H' : // heal
                if (button4Hit(x, y)) { // confirm
                    if (medpackCharge >= medpackHealAmount) {
                        medpackCharge -= medpackHealAmount;
                        int boostHeal = 0;
                        if (hero.ring1.name.equals("Ancient Ring") || hero.ring2.name.equals("Ancient Ring"))
                            boostHeal = medpackHealAmount / 2;
                        if (hero.ring1.name.equals("Ancient Ring") && hero.ring2.name.equals("Ancient Ring"))
                            boostHeal = medpackHealAmount;
                        hero.life += medpackHealAmount + boostHeal;
                        flashCell(hero.x, hero.y, Color.BLUE, fxDelay * 5);
                        clearMessages();
                        displayMessage(String.format("Gained %d life.", medpackHealAmount + boostHeal));
                        gameState = 'M';
                        if (hero.blindTimer < blindDuration) hero.blindTimer++;
                        moveMonsters();
                        drawMap();
                        drawDefaultButtons();
                    } else {
                        clearMessages();
                        displayMessage("Your flask is empty.");
                        gameState = 'M';
                        drawDefaultButtons();
                        direction = 0;
                    }
                }
                if (button6Hit(x, y)) { // cancel
                    gameState = 'M';
                    clearMessages();
                    drawDefaultButtons();
                    direction = 0;
                }
                break;
            case 'W' : // warp
                if (button4Hit(x, y)) { // confirm
                    if (warpCounter < warpDelay) {
                        clearMessages();
                        displayMessage("Still recharging.");
                        gameState = 'M';
                        drawDefaultButtons();
                        direction = 0;
                        break;
                    }
                    boolean didWarp = false;
                    for (Room r : rooms)
                        if (hero.x > r.x1 && hero.x < r.x2 && hero.y > r.y1 && hero.y < r.y2) {
                            doWarp(r.x1, r.y1, r.x2, r.y2, hero.x, hero.y);
                            didWarp = true;
                            break;
                        }
                    if (!didWarp) doWarp(hero.x, hero.y, hero.x, hero.y, hero.x, hero.y);
                    gameState = 'M';
                    clearMessages();
                    if (hero.blindTimer < blindDuration) hero.blindTimer++;
                    moveMonsters();
                    drawMap();
                    drawDefaultButtons();
                }
                if (button6Hit(x, y)) { // cancel
                    gameState = 'M';
                    clearMessages();
                    drawDefaultButtons();
                    direction = 0;
                }
                break;
            case 'C' : // chest
                if (button4Hit(x, y)) { // confirm
                    if (pickUpChest()) {
                        items.get(selectedItem).name = "empty";
                        moveMonsters();
                        drawMap();
                    }
                    gameState = 'M';
                    drawDefaultButtons();
                }
                if (button6Hit(x, y)) { // cancel
                    clearMessages();
                    gameState = 'M';
                    drawDefaultButtons();
                }
                break;
            case 'E' : // equipment
                if (arrowUpHit(x, y)) { // up
                    ptrPosition = ptrPosition == 0 ? 7 : ptrPosition - 1;
                    clearFrame(5, 6, 7, 13);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                }
                if (arrowDownHit(x, y)) { // down
                    ptrPosition = ptrPosition == 7 ? 0 : ptrPosition + 1;
                    clearFrame(5, 6, 7, 13);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                }
                if (button2Hit(x, y)) { // info
                    String s = "";
                    switch (ptrPosition) {
                        case 0 : s = hero.head.name;    showItemInfo(hero.head.name,    'W'); break;
                        case 1 : s = hero.chest.name;   showItemInfo(hero.chest.name,   'W'); break;
                        case 2 : s = hero.glove.name;   showItemInfo(hero.glove.name,   'W'); break;
                        case 3 : s = hero.legs.name;    showItemInfo(hero.legs.name,    'W'); break;
                        case 4 : s = hero.ring1.name;   showItemInfo(hero.ring1.name,   'W'); break;
                        case 5 : s = hero.ring2.name;   showItemInfo(hero.ring2.name,   'W'); break;
                        case 6 : s = hero.firearm.name; showItemInfo(hero.firearm.name, 'G'); break;
                        case 7 : s = hero.tool.name;    showItemInfo(hero.tool.name,    'T'); break;
                    }
                    if (s.equals("empty") || s.isEmpty()) break;
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    gameState = 'O';
                }
                if (button3Hit(x, y) && hero.paralyzeTimer >= paralyzeDuration) { // remove
                    Item item   = new Item();
                    item.amount = 1;
                    String aName    = "";
                    String action   = "";
                    int    aAmount  = 0;
                    int    invItems = 0;
                    for (Item i : items) if (i.x == 0) invItems++;
                    switch (ptrPosition) {
                        case 0 : // head
                                 item.name = hero.head.name;
                                 item.tag  = 'W';
                                 hero.head = equipTypes.get(0);
                                 break;
                        case 1 : // chest
                                 item.name  = hero.chest.name;
                                 item.tag   = 'W';
                                 hero.chest = equipTypes.get(0);
                                 break;
                        case 2 : // glove
                                 item.name  = hero.glove.name;
                                 item.tag   = 'W';
                                 hero.glove = equipTypes.get(0);
                                 break;
                        case 3 : // legs
                                 item.name = hero.legs.name;
                                 item.tag  = 'W';
                                 hero.legs = equipTypes.get(0);
                                 break;
                        case 4 : // ring 1
                                 item.name  = hero.ring1.name;
                                 item.tag   = 'W';
                                 hero.ring1 = equipTypes.get(0);
                                 break;
                        case 5 : // ring 2
                                 item.name  = hero.ring2.name;
                                 item.tag   = 'W';
                                 hero.ring2 = equipTypes.get(0);
                                 break;
                        case 6 : // gun
                                 item.name = "empty";
                                 break;
/*                                 item.name      = hero.firearm.name;
                                 item.tag       = 'G';
                                 hero.firearm   = gunTypes.get(0);
                                 if (hero.ammo > 0) {
                                     aAmount   = hero.ammo;
                                     for (Ammo a : ammoTypes)
                                         if (item.name.toLowerCase().contains(a.weapon.toLowerCase())) { aName = a.name; break; }
                                     Item i    = new Item();
                                     if (invItems >= inventoryCapacity - 1) {
                                         action = "Dropped";
                                         i.x    = hero.x;
                                         i.y    = hero.y;
                                     } else {
                                         action = "Removed";
                                         i.x    = 0;
                                         i.y    = 0;
                                     }
                                     i.name    = aName;
                                     i.amount  = hero.ammo;
                                     i.tag     = 'B';
                                     items.add(i);
                                     hero.ammo = 0;
                                 }
                                 break;
                                 this shit actually works, disabled for a few reasons */
                        case 7 : // melee weapon
                                 item.name = hero.tool.name;
                                 item.tag  = 'T';
                                 hero.tool = toolTypes.get(1);
                                 break;
                    }
                    if (item.name.equals("empty") || item.name.equals(toolTypes.get(1).name)) break;
                    if (invItems >= inventoryCapacity) {
                        if (action.isEmpty()) action = "Dropped";
                        item.x = hero.x;
                        item.y = hero.y;
                    } else {
                        if (action.isEmpty()) action = "Removed";
                        item.x = 0;
                        item.y = 0;
                    }
                    if (hero.prevCell != corpse && item.x != 0) hero.prevCell = clip;
                    if (item.tag == 'G' && aAmount > 0) {
                        if (aAmount == 1) aName = aName.substring(0, aName.length() - 1);
                        displayMessage(String.format("%s %s and %d %s.", action, item.name, aAmount, aName));
                    } else
                        displayMessage(String.format("%s %s.", action, item.name));
                    items.add(item);
                    gameState = 'M';
                    ptrPosition = 0;
                    moveMonsters();
                    drawMap();
                    drawDefaultButtons();
                }
                if (button6Hit(x, y)) { // cancel
                    gameState = 'M';
                    ptrPosition = 0;
                    drawMap();
                    drawDefaultButtons();
                }
                break;
            case 'I' : // inventory
                if (arrowUpHit(x, y)) { // up
                    if (mine.size() < 1) break;
                    ptrPosition = ptrPosition == 0 ? mine.size() - 1 : ptrPosition - 1;
                    clearFrame(5, 6, 7, 18);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                }
                if (arrowDownHit(x, y)) { // down
                    if (mine.size() < 1) break;
                    ptrPosition = ptrPosition == mine.size() - 1 ? 0 : ptrPosition + 1;
                    clearFrame(5, 6, 7, 18);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                }
                if (button1Hit(x, y) && hero.paralyzeTimer >= paralyzeDuration) { // equip
                    if (mine.size() < 1) break;
                    if (mine.size() == inventoryCapacity && mine.get(ptrPosition).tag == 'G') {
                        String ammoName = "";
                        for (Ammo a : ammoTypes)
                            if (mine.get(ptrPosition).name.toLowerCase().contains(a.name.toLowerCase())) {
                                ammoName = a.name;
                                break;
                            }
                        boolean found = false;
                        for (Item i : mine)
                            if (i.name.equals(ammoName)) {
                                found = true; break;
                            }
                        if (!found && hero.ammo > 0) {
                            gameState = 'M';
                            ptrPosition = 0;
                            mine.clear();
                            drawMap();
                            drawDefaultButtons();
                            displayMessage("You're carrying too much.");
                            break;
                        }
                    }
                    if (mine.get(ptrPosition).tag == 'F' || mine.get(ptrPosition).tag == 'B' || mine.get(ptrPosition).tag == 'H') break;
                    Item swap   = new Item();
                    swap.x      = mine.get(ptrPosition).x;
                    swap.y      = mine.get(ptrPosition).y;
                    swap.name   = mine.get(ptrPosition).name;
                    swap.amount = mine.get(ptrPosition).amount;
                    swap.tag    = mine.get(ptrPosition).tag;
                    switch (swap.tag) {
                        case 'T' : // melee weapon
                                   for (Weapon w : toolTypes)
                                       if (w.name.equals(swap.name)) {
                                           if (hero.tool.name.equals(toolTypes.get(1).name)) {
                                               mine.remove(ptrPosition);
                                               for (int i = 0; i < items.size(); i++)
                                                   if (items.get(i).name.equals(swap.name) && items.get(i).x == 0) {
                                                       items.remove(i);
                                                       break;
                                                   }
                                           } else mine.get(ptrPosition).name = hero.tool.name;
                                           hero.tool = w;
                                           displayMessage(String.format("%s equipped.", capitalize(hero.tool.name)));
                                           break;
                                       }
                                   break;
                        case 'G' : // firearm
                                   // swap weapon
                                   mine.get(ptrPosition).name = hero.firearm.name;
                                   for (Weapon w : gunTypes)
                                       if (w.name.equals(swap.name)) {
                                           hero.firearm = w;
                                           displayMessage(String.format("%s equipped.", capitalize(hero.firearm.name)));
                                           break;
                                       }
                                   // swap ammo
                                   String swapAmmo = "", ammoName = "";
                                   for (Ammo a : ammoTypes) {
                                       if (swap.name.toLowerCase().contains(a.weapon.toLowerCase()))                  swapAmmo = a.name;
                                       if (mine.get(ptrPosition).name.toLowerCase().contains(a.weapon.toLowerCase())) ammoName = a.name;
                                   }
                                   boolean found = false;
                                   if (!swapAmmo.equals(ammoName)) {
                                       for (Item i : mine)
                                           if (i.name.equals(swapAmmo)) {
                                               swap        = new Item();
                                               swap.x      = i.x;
                                               swap.y      = i.y;
                                               swap.name   = i.name;
                                               swap.amount = i.amount;
                                               swap.tag    = i.tag;
                                               i.name      = ammoName;
                                               i.amount    = hero.ammo;
                                               for (int j = 0; j < items.size(); j++)
                                                   if (items.get(j).name.equals(ammoName)) {
                                                       if (hero.ammo < 1) items.remove(j);
                                                       found = true;
                                                       break;
                                                   }
                                               break;
                                           }
                                       if (!found && hero.ammo > 1) {
                                           Item item   = new Item();
                                           item.x      = 0;
                                           item.y      = 0;
                                           item.name   = ammoName;
                                           item.amount = hero.ammo;
                                           item.tag    = 'B';
                                           items.add(item);
                                           hero.ammo   = 0;
                                       }
                                       if (found) hero.ammo = swap.amount;
                                   } else {
                                       for (int i = 0; i < items.size(); i++)
                                           if (items.get(i).name.equals(swapAmmo) && items.get(i).x == 0) {
                                               hero.ammo += items.get(i).amount;
                                               items.remove(i);
                                               break;
                                           }
                                   }
                                   break;
                        case 'W' : // clothing
                                   for (Equip e : equipTypes)
                                       if (swap.name.equals(e.name)) {
                                           String mem = "empty";
                                           switch (e.type) {
                                               case "head"  : mem       = hero.head.name;
                                                              hero.head = e;
                                                              break;
                                               case "chest" : mem        = hero.chest.name;
                                                              hero.chest = e;
                                                              break;
                                               case "glove" : mem        = hero.glove.name;
                                                              hero.glove = e;
                                                              break;
                                               case "legs"  : mem        = hero.legs.name;
                                                              hero.legs  = e;
                                                              break;
                                               case "ring"  : displayMessage("Select ring slot.");
                                                              canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.leftbutt),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
                                                              canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.rightbutt), screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                                                              canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),    screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                                                              canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),    screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                                                              string    = swap.name;
                                                              gameState = 'N';
                                                              imageView.invalidate();
                                                              return;
                                           }
                                           displayMessage(String.format("%s equipped.", capitalize(swap.name)));
                                           for (int i = 0; i < items.size(); i++)
                                               if (items.get(i).name.equals(swap.name) && items.get(i).x == 0) {
                                                   if (!mem.equals("empty")) {
                                                       items.get(i).name   = mem;
                                                       items.get(i).amount = 1;
                                                       items.get(i).tag    = 'W';
                                                       items.get(i).x      = 0;
                                                       items.get(i).y      = 0;
                                                   } else
                                                       items.remove(i);
                                                   break;
                                               }
                                           break;
                                       }
                                   break;
                    }
                    gameState = 'M';
                    ptrPosition = 0;
                    mine.clear();
                    moveMonsters();
                    drawMap();
                    drawDefaultButtons();
                }
                if (button2Hit(x, y)) { // info
                    if (mine.size() < 1 || mine.get(ptrPosition).tag == 'B' || mine.get(ptrPosition).tag == 'M') break;
                    switch (mine.get(ptrPosition).tag) {
                        case 'T' : showItemInfo(mine.get(ptrPosition).name, 'T'); break;
                        case 'G' : showItemInfo(mine.get(ptrPosition).name, 'G'); break;
                        case 'F' : showItemInfo("flash bomb", 'F'); break;
                        case 'H' : showItemInfo("homeward scroll", 'H'); break;
                        case 'W' : showItemInfo(mine.get(ptrPosition).name, 'W'); break;
                    }
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    gameState = 'S';
                }
                if (button3Hit(x, y) && hero.paralyzeTimer >= paralyzeDuration) { // use
                    if (mine.size() < 1) break;
                    switch (mine.get(ptrPosition).tag) {
                        case 'F' : // flashbang
                                   for (int i = 0; i < items.size(); i++)
                                       if (items.get(i).name.equals(mine.get(ptrPosition).name)) {
                                           items.get(i).amount--;
                                           if (items.get(i).amount <= 0) items.remove(i);
                                           break;
                                       }
                                   drawMap();
                                   blindMonsters();
                                   gameState = 'M';
                                   ptrPosition = 0;
                                   mine.clear();
                                   moveMonsters();
                                   drawMap();
                                   drawDefaultButtons();
                                   break;
                        case 'H' : // homeward bone
                                   for (int i = 0; i < items.size(); i++)
                                       if (items.get(i).name.equals(mine.get(ptrPosition).name)) {
                                           items.get(i).amount--;
                                           if (items.get(i).amount <= 0) items.remove(i);
                                           break;
                                       }
                                   flashCell(hero.x, hero.y, Color.GREEN, fxDelay * 10);
                                   hero.prevCell = pass;
                                   if (hero.life < 100) hero.life = 100;
                                   monsters.clear();
                                   mine.clear();
                                   gameState = 'M';
                                   ptrPosition = 0;
                                   depth = 0;
                                   townInstance();
                                   drawDefaultButtons();
                                   break;
                    }
                }
                if (button5Hit(x, y) && hero.paralyzeTimer >= paralyzeDuration) { // drop
                    if (mine.size() < 1) break;
                    mine.get(ptrPosition).x = hero.x;
                    mine.get(ptrPosition).y = hero.y;
                    if (hero.prevCell != corpse) hero.prevCell = clip;
                    String a = mine.get(ptrPosition).name;
                    switch (mine.get(ptrPosition).tag) {
                        case 'B' :
                        case 'F' : if (mine.get(ptrPosition).amount == 1 && mine.get(ptrPosition).tag == 'B')
                                       a = a.substring(0, a.length() - 1);
                                   if (mine.get(ptrPosition).amount > 1 && mine.get(ptrPosition).tag == 'F') a += "s";
                                   displayMessage(String.format("Dropped %d %s.", mine.get(ptrPosition).amount, a));
                                   break;
                        default  : displayMessage(String.format("Dropped %s.", mine.get(ptrPosition).name));
                    }
                    gameState = 'M';
                    ptrPosition = 0;
                    mine.clear();
                    moveMonsters();
                    drawMap();
                    drawDefaultButtons();
                }
                if (button6Hit(x, y)) { // cancel
                    gameState = 'M';
                    ptrPosition = 0;
                    mine.clear();
                    drawMap();
                    drawDefaultButtons();
                }
                break;
            case 'N' : // ring slot select
                String  mem  = "empty";
                boolean done = false;
                if (button1Hit(x, y)) { // left
                    for (Equip e : equipTypes)
                        if (string.equals(e.name)) {
                            mem        = hero.ring1.name;
                            hero.ring1 = e;
                            done       = true;
                            break;
                        }
                }
                if (button2Hit(x, y)) { // right
                    for (Equip e : equipTypes)
                        if (string.equals(e.name)) {
                            mem        = hero.ring2.name;
                            hero.ring2 = e;
                            done       = true;
                            break;
                        }
                }
                if (button6Hit(x, y)) { // cancel
                    clearMessages();
                    gameState = 'I';
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.equip),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.info),   screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.use),    screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.drop),   screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                }
                if (done) {
                    displayMessage(String.format("%s equipped.", capitalize(string)));
                    for (int i = 0; i < items.size(); i++)
                        if (items.get(i).name.equals(string) && items.get(i).x == 0) {
                            if (!mem.equals("empty")) {
                                items.get(i).name   = mem;
                                items.get(i).amount = 1;
                                items.get(i).tag    = 'W';
                                items.get(i).x      = 0;
                                items.get(i).y      = 0;
                            } else
                                items.remove(i);
                            break;
                        }
                    gameState   = 'M';
                    ptrPosition = 0;
                    string      = "";
                    mine.clear();
                    moveMonsters();
                    drawMap();
                    drawDefaultButtons();
                }
                break;
            case 'S' : // item info
                if (button6Hit(x, y)) { // cancel
                    gameState = 'I';
                    listItems("Inventory");
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.equip), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.info),  screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.use),   screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.drop),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                }
                break;
            case 'T' : // tab 1 info
                if (button6Hit(x, y)) { // cancel
                    gameState = '1';
                    stashItems(1);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.info),    screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.move),    screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel),  screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                }
                break;
            case 'R' : // tab 2 info
                if (button6Hit(x, y)) { // cancel
                    gameState = '2';
                    stashItems(2);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.info),    screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.move),    screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel),  screenWidth - buttonWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                }
                break;
            case 'P' : // vendor info
                if (button6Hit(x, y)) { // cancel
                    gameState = 'V';
                    listItems("Things for sale");
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.info),    screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.buy),     screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel),  screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                }
                break;
            case 'O' : // equip screen info
                if (button6Hit(x, y)) { // cancel
                    gameState = 'E';
                    listEquipment();
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.info),   screenWidth - buttonWidth,                 27 * tileHeight + tileHeight / 2,                   null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.remove), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight + tileHeight * 1.5f, null);
                    break;
                }
            case 'V' : // vendor
                clearMessages();
                if (arrowUpHit(x, y)) { // up
                    if (mine.size() < 1) break;
                    ptrPosition = ptrPosition == 0 ? mine.size() - 1 : ptrPosition - 1;
                    clearFrame(5, 6, 7, 18);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                }
                if (arrowDownHit(x, y)) { // down
                    if (mine.size() < 1) break;
                    ptrPosition = ptrPosition == mine.size() - 1 ? 0 : ptrPosition + 1;
                    clearFrame(5, 6, 7, 18);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                }
                if (button2Hit(x, y)) { // info
                    if (mine.size() < 1 || mine.get(ptrPosition).tag == 'B' || mine.get(ptrPosition).tag == 'M' || mine.get(ptrPosition).tag == 'R') break;
                    switch (mine.get(ptrPosition).tag) {
                        case 'T' : showItemInfo(mine.get(ptrPosition).name, 'T'); break;
                        case 'G' : showItemInfo(mine.get(ptrPosition).name, 'G'); break;
                        case 'F' : showItemInfo("flash bomb", 'F'); break;
                        case 'H' : showItemInfo("homeward scroll", 'H'); break;
                        case 'W' : showItemInfo(mine.get(ptrPosition).name, 'W'); break;
                    }
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    gameState = 'P';
                }
                if (button4Hit(x, y)) { // confirm
                    if (mine.size() < 1) break;
                    if (mine.get(ptrPosition).price * mine.get(ptrPosition).amount > hero.gold) {
                        displayMessage("You can't afford it.");
                        break;
                    }
                    String  a     = mine.get(ptrPosition).name;
                    boolean found = false;
                    int     inventoryItemCount = 0;
                    switch (mine.get(ptrPosition).tag) {
                        case 'H' : // homeward scroll
                        case 'F' : // flashbang
                                   for (Item i : items) // search bag
                                       if (i.name.equals(mine.get(ptrPosition).name) && i.x == 0) { // got it
                                           i.amount  += mine.get(ptrPosition).amount;
                                           found      = true;
                                       }
                                   break;
                        case 'R' : // flask refill
                                   if (medpackCharge >= medpackCapacity) {
                                       displayMessage("Your flask is full.");
                                       imageView.invalidate();
                                       return;
                                   }
                                   medpackCharge = medpackCapacity;
                                   found         = true;
                                   break;
                        case 'B' : // ammo
                                   String wName = "";
                                   for (Ammo w : ammoTypes)
                                       if (mine.get(ptrPosition).name.equals(w.name)) {
                                           wName = w.weapon;
                                           break;
                                       }
                                   if (hero.firearm.name.toLowerCase().contains(wName.toLowerCase())) { // ammo for my gun?
                                       hero.ammo += mine.get(ptrPosition).amount;
                                       found      = true;
                                   }
                                   if (!found)
                                       for (Item i : items) // bagcheck
                                           if (i.name.equals(mine.get(ptrPosition).name) && i.x == 0) {
                                               i.amount += mine.get(ptrPosition).amount;
                                               found     = true;
                                               break;
                                           }
                    }
                    if (!found) {
                        for (Item i : items) if (i.x == 0) inventoryItemCount++;
                        if (inventoryItemCount >= inventoryCapacity) {
                            displayMessage("Your inventory is full.");
                            break;
                        }
                        Item item   = new Item();
                        item.x      = 0;
                        item.y      = 0;
                        item.name   = mine.get(ptrPosition).name;
                        item.amount = mine.get(ptrPosition).amount;
                        item.price  = mine.get(ptrPosition).price;
                        item.tag    = mine.get(ptrPosition).tag;
                        items.add(item);
                        found = true;
                    }
                    if (found) {
                        if (mine.get(ptrPosition).tag == 'B' && mine.get(ptrPosition).amount == 1) a = a.substring(0, a.length() - 1);
                        hero.gold -= mine.get(ptrPosition).price * mine.get(ptrPosition).amount;
                        displayMessage("\"Thank you kindly.\"");
                        displayMessage(String.format("Purchased %d %s.", mine.get(ptrPosition).amount, a));
                        listItems("Things for sale");
                    }
                }
                if (button6Hit(x, y)) { // cancel
                    gameState = 'M';
                    ptrPosition = 0;
                    mine.clear();
                    drawMap();
                    drawDefaultButtons();
                    displayMessage("\"See you around.\"");
                }
                break;
            case '1' : // stash tab 1
                clearMessages();
                if (arrowUpHit(x, y)) { // up
                    if (mine.size() < 1) break;
                    ptrPosition = ptrPosition == 0 ? mine.size() - 1 : ptrPosition - 1;
                    clearFrame(5, 6, 7, 18);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                }
                if (arrowDownHit(x, y)) { // down
                    if (mine.size() < 1) break;
                    ptrPosition = ptrPosition == mine.size() - 1 ? 0 : ptrPosition + 1;
                    clearFrame(5, 6, 7, 18);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                }
                if (arrowRightHit(x, y)) { // right
                    gameState   = '2';
                    ptrPosition = 0;
                    stashItems(2);
                    break;
                }
                if (button2Hit(x, y)) { // info
                    if (mine.size() < 1 || mine.get(ptrPosition).tag == 'B' || mine.get(ptrPosition).tag == 'M') break;
                    switch (mine.get(ptrPosition).tag) {
                        case 'T' : showItemInfo(mine.get(ptrPosition).name, 'T'); break;
                        case 'G' : showItemInfo(mine.get(ptrPosition).name, 'G'); break;
                        case 'F' : showItemInfo("flash bomb", 'F'); break;
                        case 'H' : showItemInfo("homeward scroll", 'H'); break;
                        case 'W' : showItemInfo(mine.get(ptrPosition).name, 'W'); break;
                    }
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    gameState = 'T';
                }
                if (button4Hit(x, y)) { // confirm
                    if (mine.size() < 1) break;
                    boolean found = false;
                    String  a     = mine.get(ptrPosition).name;
                    switch (mine.get(ptrPosition).tag) {
                        case 'B' : // ammo
                        case 'F' : // flash bomb
                        case 'H' : // homeward scroll
                                   if (mine.get(ptrPosition).amount > 1 && (mine.get(ptrPosition).tag == 'F' || mine.get(ptrPosition).tag == 'H')) a += 's';
                                   if (mine.get(ptrPosition).amount == 1 && mine.get(ptrPosition).tag == 'B') a = a.substring(0, a.length() - 1);
                                   for (Item i : stashed)
                                       if (i.name.equals(mine.get(ptrPosition).name)) { // found in stash
                                           i.amount += mine.get(ptrPosition).amount;
                                           displayMessage(String.format("%d %s added to stash.", mine.get(ptrPosition).amount, a));
                                           mine.get(ptrPosition).name = "empty";
                                           found = true;
                                           break;
                                       }
                    }
                    if (!found) {
                        if (stashed.size() >= inventoryCapacity) {
                            displayMessage("Your stash is full.");
                            break;
                        }
                        mine.get(ptrPosition).x = 100;
                        mine.get(ptrPosition).y = 100;
                        if (mine.get(ptrPosition).tag == 'B' || mine.get(ptrPosition).tag == 'F' || mine.get(ptrPosition).tag == 'H')
                            displayMessage(String.format("%d %s moved to stash.", mine.get(ptrPosition).amount, a));
                        else
                            displayMessage(String.format("%s moved to stash.", capitalize(mine.get(ptrPosition).name)));
                        found = true;
                    }
                    if (found) {
                        cleanUpItems(0, 0);
                        stashItems(1);
                        clearFrame(5, 6, 7, 18);
                        if (mine.size() < 1) break;
                        if (ptrPosition > mine.size() - 1) ptrPosition--;
                        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                    }
                }
                if (button6Hit(x, y)) { // cancel
                    gameState = 'M';
                    ptrPosition = 0;
                    mine.clear();
                    stashed.clear();
                    drawMap();
                    drawDefaultButtons();
                }
                break;
            case '2' : // stash tab 2
                clearMessages();
                if (arrowUpHit(x, y)) { // up
                    if (stashed.size() < 1) break;
                    ptrPosition = ptrPosition == 0 ? stashed.size() - 1 : ptrPosition - 1;
                    clearFrame(6, 7, 8, 19);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 7 * tileWidth, (7 + ptrPosition) * tileHeight + 8, null);
                }
                if (arrowDownHit(x, y)) { // down
                    if (stashed.size() < 1) break;
                    ptrPosition = ptrPosition == stashed.size() - 1 ? 0 : ptrPosition + 1;
                    clearFrame(6, 7, 8, 19);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 7 * tileWidth, (7 + ptrPosition) * tileHeight + 8, null);
                }
                if (arrowLeftHit(x, y)) { // left
                    gameState   = '1';
                    ptrPosition = 0;
                    stashItems(1);
                    break;
                }
                if (button2Hit(x, y)) { // info
                    if (stashed.size() < 1 || stashed.get(ptrPosition).tag == 'B' || stashed.get(ptrPosition).tag == 'M') break;
                    switch (stashed.get(ptrPosition).tag) {
                        case 'T' : showItemInfo(stashed.get(ptrPosition).name, 'T'); break;
                        case 'G' : showItemInfo(stashed.get(ptrPosition).name, 'G'); break;
                        case 'F' : showItemInfo("flash bomb", 'F'); break;
                        case 'H' : showItemInfo("homeward scroll", 'H'); break;
                        case 'W' : showItemInfo(stashed.get(ptrPosition).name, 'W'); break;
                    }
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
                    gameState = 'R';
                }
                if (button4Hit(x, y)) { // confirm
                    if (stashed.size() < 1) break;
                    boolean found = false;
                    String  a     = stashed.get(ptrPosition).name;
                    switch (stashed.get(ptrPosition).tag) {
                        case 'B' : // ammo
                        case 'F' : // flash bomb
                        case 'H' : // homeward scroll
                                   if (stashed.get(ptrPosition).amount > 1 && (stashed.get(ptrPosition).tag == 'F' || stashed.get(ptrPosition).tag == 'H')) a += 's';
                                   if (stashed.get(ptrPosition).amount == 1 && stashed.get(ptrPosition).tag == 'B') a = a.substring(0, a.length() - 1);
                                   for (Item i : mine)
                                       if (i.name.equals(stashed.get(ptrPosition).name)) { // found in stash
                                           i.amount += stashed.get(ptrPosition).amount;
                                           displayMessage(String.format("%d %s added to inventory.", stashed.get(ptrPosition).amount, a));
                                           stashed.get(ptrPosition).name = "empty";
                                           found = true;
                                           break;
                                       }
                    }
                    if (!found) {
                        if (mine.size() >= inventoryCapacity) {
                            displayMessage("Your inventory is full.");
                            break;
                        }
                        stashed.get(ptrPosition).x = 0;
                        stashed.get(ptrPosition).y = 0;
                        if (stashed.get(ptrPosition).tag == 'B' || stashed.get(ptrPosition).tag == 'F' || stashed.get(ptrPosition).tag == 'H')
                            displayMessage(String.format("%d %s moved to inventory.", stashed.get(ptrPosition).amount, a));
                        else
                            displayMessage(String.format("%s moved to inventory.", capitalize(stashed.get(ptrPosition).name)));
                        found = true;
                    }
                    if (found) {
                        cleanUpItems(100, 100);
                        stashItems(2);
                        clearFrame(6, 7, 8, 19);
                        if (stashed.size() < 1) break;
                        if (ptrPosition > stashed.size() - 1) ptrPosition--;
                        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 7 * tileWidth, (7 + ptrPosition) * tileHeight + 8, null);
                    }
                }
                if (button6Hit(x, y)) { // cancel
                    gameState = 'M';
                    ptrPosition = 0;
                    mine.clear();
                    stashed.clear();
                    drawMap();
                    drawDefaultButtons();
                }
                break;
            case 'X' : // waypoint
                if (arrowUpHit(x, y)) { // up
                    ptrPosition = ptrPosition == 0 ? progression / 10 : ptrPosition - 1;
                    clearFrame(15, 8, 16, 17);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 15 * tileWidth, (8 + ptrPosition) * tileHeight + 8, null);
                }
                if (arrowDownHit(x, y)) { // down
                    ptrPosition = ptrPosition == progression / 10 ? 0 : ptrPosition + 1;
                    clearFrame(15, 8, 16, 17);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 15 * tileWidth, (8 + ptrPosition) * tileHeight + 8, null);
                }
                if (button4Hit(x, y)) { // confirm
                    gameState = 'M';
                    depth = ptrPosition * 10 + 1;
                    ptrPosition = 0;
                    newInstance();
                    drawDefaultButtons();
                    displayMessage("Survive the dungeon and escape!");
                }
                if (button6Hit(x, y)) { // cancel
                    gameState = 'M';
                    ptrPosition = 0;
                    drawMap();
                    drawDefaultButtons();
                }
                break;
            case 'Y' : // confirm to pick shit up from the ground
                if (button4Hit(x, y)) { // confirm
                    gameState = 'G';
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.select), screenWidth - buttonWidth, 27 * tileHeight + tileHeight / 2, null);
                    for (Item i : items)
                        if (i.x == hero.x && i.y == hero.y && !i.name.equals("empty")) {
                            i.marked = false;
                            mine.add(i);
                        }
                    clearMessages();
                    listItems("Select item(s)");
                }
                if (button6Hit(x, y)) { // cancel
                    if (hero.prevCell != corpse) hero.prevCell = clip;
                    gameState = 'M';
                    clearMessages();
                    drawMap();
                    drawDefaultButtons();
                }
                break;
            case 'G' : // manage stuff on the ground
                if (arrowUpHit(x, y)) { // up
                    if (mine.size() < 1) break;
                    ptrPosition = ptrPosition == 0 ? mine.size() - 1 : ptrPosition - 1;
                    clearFrame(5, 6, 7, 18);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                }
                if (arrowDownHit(x, y)) { // down
                    if (mine.size() < 1) break;
                    ptrPosition = ptrPosition == mine.size() - 1 ? 0 : ptrPosition + 1;
                    clearFrame(5, 6, 7, 18);
                    canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                }
                if (button2Hit(x, y)) { // select
                    mine.get(ptrPosition).marked = !mine.get(ptrPosition).marked;
                    if (mine.get(ptrPosition).marked)
                        paint.setColor(Color.parseColor("#90EE90")); // light green
                    else paint.setColor(Color.LTGRAY);
                    if (mine.get(ptrPosition).tag == 'B' || mine.get(ptrPosition).tag == 'F')
                        canvas.drawText(String.format("%s (%d)", mine.get(ptrPosition).name, mine.get(ptrPosition).amount),
                                        7 * tileWidth, (7 + ptrPosition) * tileHeight, paint);
                    else canvas.drawText(mine.get(ptrPosition).name, 7 * tileWidth, (7 + ptrPosition) * tileHeight, paint);
                }
                if (button4Hit(x, y)) { // confirm
                    int     bagItemCount = 0;
                    int     markedCount  = 0;
                    boolean didPickUp    = false;
                    for (Item i : items) if (i.x == 0) bagItemCount++;
                    for (Item i : mine)  if (i.marked) markedCount++;
                    if (markedCount + bagItemCount > inventoryCapacity) {
                        displayMessage("You're carrying too much.");
                        ptrPosition = 0;
                        gameState = 'M';
                        mine.clear();
                        moveMonsters();
                        drawMap();
                        drawDefaultButtons();
                        break;
                    }
                    for (Item i : mine) {
                        if (!i.marked) continue;
                        boolean found = false;
                        String  a     = "";
                        switch (i.tag) {
                            case 'T' : // melee weapon
                            case 'G' : // gun
                            case 'W' : // clothes
                            case 'M' : // keycard
                                       i.x = 0;
                                       i.y = 0;
                                       displayMessage(String.format("Picked up %s.", i.name));
                                       didPickUp = true;
                                       break;
                            case 'H' : // homeward scroll
                            case 'F' : // flashbang
                                       for (Item j : items) // search bag
                                           if (j.name.equals(i.name) && j.x == 0) {
                                               j.amount += i.amount;
                                               found = true;
                                               break;
                                           }
                                       if (!found) { // don't have
                                           i.x = 0;
                                           i.y = 0;
                                       }
                                       a = i.name;
                                       if (i.amount > 1) a += "s";
                                       displayMessage(String.format("Picked up %d %s.", i.amount, a));
                                       didPickUp = true;
                                       if (found) i.name = "empty";
                                       break;
                            case 'B' : // ammo
                                       String wName = "";
                                       for (Ammo w : ammoTypes)
                                           if (i.name.equals(w.name)) {
                                               wName = w.weapon;
                                               break;
                                           }
                                       if (hero.firearm.name.toLowerCase().contains(wName.toLowerCase())) { // is it for my equipped gun?
                                           hero.ammo += i.amount;
                                           found = true;
                                       }
                                       if (!found)
                                           for (Item j : items) // do I have it in my bag?
                                               if (j.name.equals(i.name) && j.x == 0) {
                                                   j.amount += i.amount;
                                                   found = true;
                                                   break;
                                               }
                                       if (!found) { // I don't have it
                                           i.x = 0;
                                           i.y = 0;
                                       }
                                       a = i.name;
                                       if (i.amount == 1) a = a.substring(0, a.length() - 1);
                                       displayMessage(String.format("Picked up %d %s.", i.amount, a));
                                       didPickUp = true;
                                       if (found) i.name = "empty";
                        }
                    }
                    cleanUpItems(hero.x, hero.y);
                    if (hero.prevCell != corpse) {
                        hero.prevCell = pass;
                        for (Item i : items)
                            if (i.x == hero.x && i.y == hero.y && !i.name.equals("empty")) {
                                hero.prevCell = hero.prevCell != corpse ? clip : corpse;
                                break;
                            }
                    }
                    ptrPosition = 0;
                    gameState = 'M';
                    mine.clear();
                    drawMap();
                    drawDefaultButtons();
                    if (didPickUp) {
                        if (hero.blindTimer < blindDuration) hero.blindTimer++;
                        moveMonsters();
                        drawMap();
                    }
                }
                if (button6Hit(x, y)) { // cancel
                    hero.prevCell = hero.prevCell != corpse ? clip : corpse;
                    ptrPosition = 0;
                    gameState = 'M';
                    clearMessages();
                    mine.clear();
                    moveMonsters();
                    drawMap();
                    drawDefaultButtons();
                }
                break;
        }
        statusUpdate();
        imageView.invalidate();
    }

    private boolean arrowHomeHit(float x, float y) {
        return x > 0 && y > 27 * tileHeight && x < arrowWidth && y < 27 * tileHeight + arrowHeight;
    }

    private boolean arrowUpHit(float x, float y) {
        return x > arrowWidth && y > 27 * tileHeight && x < arrowWidth * 2 && y < 27 * tileHeight + arrowHeight;
    }

    private boolean arrowPgUpHit(float x, float y) {
        return x > arrowWidth * 2 && y > 27 * tileHeight && x < arrowWidth * 3 && y < 27 * tileHeight + arrowHeight;
    }

    private boolean arrowLeftHit(float x, float y) {
        return x > 0 && y > 27 * tileHeight + arrowHeight && x < arrowWidth && y < 27 * tileHeight + arrowHeight * 2;
    }

    private boolean arrowWaitHit(float x, float y) {
        return x > arrowWidth && y > 27 * tileHeight + arrowHeight && x < arrowWidth * 2 && y < 27 * tileHeight + arrowHeight * 2;
    }

    private boolean arrowRightHit(float x, float y) {
        return x > arrowWidth * 2 && y > 27 * tileHeight + arrowHeight && x < arrowWidth * 3 && y < 27 * tileHeight + arrowHeight * 2;
    }

    private boolean arrowEndHit(float x, float y) {
        return x > 0 && y > 27 * tileHeight + arrowHeight * 2 && x < arrowWidth && y < 27 * tileHeight + arrowHeight * 3;
    }

    private boolean arrowDownHit(float x, float y) {
        return x > arrowWidth && y > 27 * tileHeight + arrowHeight *2 && x < arrowWidth * 2 && y < 27 * tileHeight + arrowHeight * 3;
    }

    private boolean arrowPgDnHit(float x, float y) {
        return x > arrowWidth * 2 && y > 27 * tileHeight + arrowHeight * 2 && x < arrowWidth * 3 && y < 27 * tileHeight + arrowHeight * 3;
    }

    private boolean button1Hit(float x, float y) {
        return x > screenWidth - buttonWidth * 2 - tileWidth && y > 27 * tileHeight                + tileHeight / 2 &&
               x < screenWidth - buttonWidth     - tileWidth && y < 27 * tileHeight + buttonHeight + tileHeight / 2;
    }

    private boolean button2Hit(float x, float y) {
        return x > screenWidth - buttonWidth && y > 27 * tileHeight                + tileHeight / 2 &&
               x < screenWidth               && y < 27 * tileHeight + buttonHeight + tileHeight / 2;
    }

    private boolean button3Hit(float x, float y) {
        return x > screenWidth - buttonWidth * 2 - tileWidth && y > 27 * tileHeight + buttonHeight     + tileHeight * 1.5f &&
               x < screenWidth - buttonWidth     - tileWidth && y < 27 * tileHeight + buttonHeight * 2 + tileHeight * 1.5f;
    }

    private boolean button4Hit(float x, float y) {
        return x > screenWidth - buttonWidth && y > 27 * tileHeight + buttonHeight     + tileHeight * 1.5f &&
               x < screenWidth               && y < 27 * tileHeight + buttonHeight * 2 + tileHeight * 1.5f;
    }

    private boolean button5Hit(float x, float y) {
        return x > screenWidth - buttonWidth * 2 - tileWidth && y > 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f &&
               x < screenWidth - buttonWidth     - tileWidth && y < 27 * tileHeight + buttonHeight * 3 + tileHeight * 2.5f;
    }

    private boolean button6Hit(float x, float y) {
        return x > screenWidth - buttonWidth && y > 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f &&
               x < screenWidth               && y < 27 * tileHeight + buttonHeight * 3 + tileHeight * 2.5f;
    }

    private void inspectCell(int x, int y) {
        clearMessages();
        if (!map[y][x].revealed) {
            displayMessage("Unrevealed spot.");
            return;
        }
        switch (map[y][x].ch) {
            case wall     : displayMessage("It's a wall."); return;
            case space    : displayMessage("Who knows what's out there."); return;
            case pass     : displayMessage("It's a floor."); return;
            case door     : displayMessage("Black fog passage."); return;
            case corpse   : displayMessage("A dead body."); return;
            case chest    : displayMessage("A treasure chest!"); return;
            case clip     : displayMessage("Something you dropped earlier."); return;
            case waypoint : displayMessage("A strange device."); return;
            case vendor   : displayMessage("Shopkeeper Grindstead. He wants your gold."); return;
            case stash    : displayMessage("Your private stash."); return;
            case stone    : displayMessage("Human-sized stone with engraved writings."); return;
            case gate     : displayMessage("Some massive gate."); return;
        }
        if (map[y][x].ch == hero.face) {
            displayMessage(String.format("%s. Loaded with power.", hero.name));
            return;
        }
        if (Character.isLetter(map[y][x].ch))
            for (Monster m : monsterTypes)
                if (map[y][x].ch == m.face) {
                    String damageType = "";
                    int    damage = 0, modifier = 0;
                    displayMessage(capitalize(m.name + '.'));
                    for (Weapon w : gunTypes)
                        if (w.name.equals(m.weapon)) {
                            damageType = "(ranged)";
                            damage     = w.damage;
                            modifier   = w.modifier;
                            break;
                        }
                    if (damageType.isEmpty())
                        for (Weapon w : toolTypes)
                            if (w.name.equals(m.weapon)) {
                                damageType = "(melee)";
                                damage     = w.damage;
                                modifier   = w.modifier;
                                break;
                            }
                    if (modifier > 0)
                        displayMessage(String.format("Damage: %d-%d %s", damage - modifier, damage + modifier, damageType));
                    else
                        displayMessage(String.format("Damage: %d %s", damage, damageType));
                    switch (m.face) {
                        case 'k' : displayMessage("Chance to paralyze."); break;
                        case 'v' : displayMessage("Leeches life."); break;
                        case 'W' : displayMessage("Chance to blind."); break;
                        case 'S' : displayMessage("Chance to avoid hits."); break;
                        case 'D' : displayMessage("Chance of additional damage."); break;
                        case 'G' : displayMessage("Chance to paralyze."); break;
                        case 'E' : displayMessage("Chance to blind."); break;
                        case 'L' : displayMessage("Chance to warp next to the player."); break;
                        case 'X' : displayMessage("Chance to reflect damage."); break;
                    }
                    break;
                }
    }

    private String tileInfo(int x, int y) {
        if (!map[y][x].revealed) return "unrevealed";
        if (hero.blindTimer < blindDuration) return "???";
        switch (map[y][x].ch) {
            case wall     : return "wall";
            case space    : return "unrevealed";
            case pass     : return "floor";
            case door     : return "black fog";
            case corpse   : return "dead body";
            case chest    : return "chest";
            case clip     : return "item pile";
            case waypoint : return "device";
            case vendor   : return "shopkeeper";
            case stash    : return "stash";
            case stone    : return "stone";
            case gate     : return "gate";
        }
        if (map[y][x].ch == hero.face) return "yourself";
        if (Character.isLetter(map[y][x].ch))
            for (Monster m : monsterTypes)
                if (map[y][x].ch == m.face) return m.name;
        return "wtf";
    }

    private boolean connectXY(int x1, int y1, int x2, int y2, char mode) {
        int deltaX = Math.abs(x2 - x1);
        int deltaY = Math.abs(y2 - y1);
        int signX  = x1 < x2 ? 1 : -1;
        int signY  = y1 < y2 ? 1 : -1;
        int error  = deltaX - deltaY;
        while (x1 != x2 || y1 != y2) {
            if (mode == 'h') map[y1][x1].highlighted = true;
            int error2 = error * 2;
            if (error2 > -deltaY) {
                error -= deltaY;
                x1    += signX;
            }
            if (error2 < deltaX) {
                error += deltaX;
                y1    += signY;
            }
            if (mode == 'a' && (map[y1][x1].ch == wall || map[y1][x1].ch == door || Character.isLetter(map[y1][x1].ch))) {
                if (doDamage(x1, y1, 'r')) map[y1][x1].ch = corpse;
                return true;
            }
            if (mode == 'c' && (map[y1][x1].ch == wall || map[y1][x1].ch == door || Character.isLetter(map[y1][x1].ch))) {
                return false;
            }
            if (mode == 'm' && (map[y1][x1].ch == wall || map[y1][x1].ch == door || Character.isLetter(map[y1][x1].ch) || map[y1][x1].ch == chest)) {
                return false;
            }
            if (mode == 'p') {
                nextX = x1;
                nextY = y1;
                return true;
            }
        }
        if (mode == 'a') doDamage(x2, y2, 'r');
        if (mode == 'h') map[y2][x2].highlighted = true;
        return true;
    }

    private void cleanUpItems(int x, int y) {
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).x == x && items.get(i).y == y && items.get(i).name.equals("empty"))
                items.remove(i);
    }

    private void listItems(String header) {
        if (header.equals("Inventory") || header.equals("Things for sale"))
            drawFrame(4, 5, 36, 19, header, String.format("Gold: %d", hero.gold));
        else
            drawFrame(4, 5, 36, 19, header, "");
        paint.setColor(Color.LTGRAY);
        if (mine.size() < 1) {
            canvas.drawText("Nothing here.", 7 * tileWidth, 7 * tileHeight, paint);
            return;
        }
        for (int i = 0; i < mine.size(); i++)
            if (mine.get(i).tag == 'B' || mine.get(i).tag == 'F' || mine.get(i).tag == 'H' || mine.get(i).tag == 'R')
                if (header.equals("Things for sale"))
                    canvas.drawText(String.format("%s (%d) - %dg", mine.get(i).name, mine.get(i).amount, mine.get(i).price * mine.get(i).amount), 7 * tileWidth, (7 + i) * tileHeight, paint);
                else
                    canvas.drawText(String.format("%s (%d)", mine.get(i).name, mine.get(i).amount), 7 * tileWidth, (7 + i) * tileHeight, paint);
            else canvas.drawText(mine.get(i).name, 7 * tileWidth, (7 + i) * tileHeight, paint);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
    }

    private void listEquipment() {
        String s = "";

        drawFrame(4, 5, 36, 14, "Equipment", "");
        paint.setColor(Color.LTGRAY);
        canvas.drawText(String.format("Head: %s", s = hero.head.name.equals("empty") ? "" : hero.head.name), 7 * tileWidth, 7 * tileHeight, paint);
        canvas.drawText(String.format("Chest: %s", s = hero.chest.name.equals("empty") ? "" : hero.chest.name), 7 * tileWidth, 8 * tileHeight, paint);
        canvas.drawText(String.format("Gloves: %s", s = hero.glove.name.equals("empty") ? "" : hero.glove.name), 7 * tileWidth, 9 * tileHeight, paint);
        canvas.drawText(String.format("Legs: %s", s = hero.legs.name.equals("empty") ? "" : hero.legs.name), 7 * tileWidth, 10 * tileHeight, paint);
        canvas.drawText(String.format("Left ring: %s", s = hero.ring1.name.equals("empty") ? "" : hero.ring1.name), 7 * tileWidth, 11 * tileHeight, paint);
        canvas.drawText(String.format("Right ring: %s", s = hero.ring2.name.equals("empty") ? "" : hero.ring2.name), 7 * tileWidth, 12 * tileHeight, paint);
        canvas.drawText(String.format("Ranged: %s", s = hero.firearm.name.equals("empty") ? "" : hero.firearm.name), 7 * tileWidth, 13 * tileHeight, paint);
        canvas.drawText(String.format("Melee: %s", s = hero.tool.name.equals("empty") ? "" : hero.tool.name), 7 * tileWidth, 14 * tileHeight, paint);
        paint.setColor(Color.WHITE);
        canvas.drawText("Head:", 7 * tileWidth, 7 * tileHeight, paint);
        canvas.drawText("Chest:", 7 * tileWidth, 8 * tileHeight, paint);
        canvas.drawText("Gloves:", 7 * tileWidth, 9 * tileHeight, paint);
        canvas.drawText("Legs:", 7 * tileWidth, 10 * tileHeight, paint);
        canvas.drawText("Left ring:", 7 * tileWidth, 11 * tileHeight, paint);
        canvas.drawText("Right ring:", 7 * tileWidth, 12 * tileHeight, paint);
        canvas.drawText("Ranged:", 7 * tileWidth, 13 * tileHeight, paint);
        canvas.drawText("Melee:", 7 * tileWidth, 14 * tileHeight, paint);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
    }

    private void stashItems(int page) {
        mine.clear();
        stashed.clear();
        for (Item i : items) {
            if (i.x == 0)   mine.add(i);
            if (i.x == 100) stashed.add(i);
        }
        switch (page) {
            case 1 : gameState = '1';
                     drawFrame(5, 6, 37, 20, "Stash", "< Inventory");
                     drawFrame(4, 5, 36, 19, "Inventory", "Stash >");
                     paint.setColor(Color.LTGRAY);
                     if (mine.size() < 1) {
                         canvas.drawText("Nothing here.", 7 * tileWidth, 7 * tileHeight, paint);
                         return;
                     }
                     for (int i = 0; i < mine.size(); i++)
                         if (mine.get(i).tag == 'B' || mine.get(i).tag == 'F' || mine.get(i).tag == 'H')
                             canvas.drawText(String.format("%s (%d)", mine.get(i).name, mine.get(i).amount), 7 * tileWidth, (7 + i) * tileHeight, paint);
                         else canvas.drawText(mine.get(i).name, 7 * tileWidth, (7 + i) * tileHeight, paint);
                     canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 6 * tileWidth, (6 + ptrPosition) * tileHeight + (tileHeight / 2) - 4, null);
                     break;
            case 2 : gameState = '2';
                     drawFrame(4, 5, 36, 19, "Inventory", "Stash >");
                     drawFrame(5, 6, 37, 20, "Stash", "< Inventory");
                     paint.setColor(Color.LTGRAY);
                     if (stashed.size() < 1) {
                         canvas.drawText("Nothing here.", 8 * tileWidth, 8 * tileHeight, paint);
                         return;
                     }
                     for (int i = 0; i < stashed.size(); i++)
                         if (stashed.get(i).tag == 'B' || stashed.get(i).tag == 'F' || stashed.get(i).tag == 'H')
                             canvas.drawText(String.format("%s (%d)", stashed.get(i).name, stashed.get(i).amount), 8 * tileWidth, (8 + i) * tileHeight, paint);
                         else canvas.drawText(stashed.get(i).name, 8 * tileWidth, (8 + i) * tileHeight, paint);
                     canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 7 * tileWidth, (7 + ptrPosition) * tileHeight + 8, null);
                     break;
        }
    }

    private void showItemInfo(String name, char tag) {
        int    damage = 0, modifier = 0;
        String type = "";
        int    tier = 0;

        if (name.equals("empty") || name.isEmpty()) return;
        switch (tag) {
            case 'T' : for (Weapon w : toolTypes)
                           if (w.name.equals(name)) {
                               damage   = w.damage;
                               modifier = w.modifier;
                               tier     = w.tier;
                               break;
                           }
                       break;
            case 'G' : for (Weapon w : gunTypes)
                           if (w.name.equals(name)) {
                               damage   = w.damage;
                               modifier = w.modifier;
                               tier     = w.tier;
                               break;
                           }
                       break;
            case 'W' : for (Equip e : equipTypes)
                           if (e.name.equals(name)) {
                               modifier = e.armorValue;
                               type     = e.type;
                               tier     = e.tier;
                               break;
                           }
                       break;
        }
        if (tier == 10 && !type.equals("ring"))
            drawFrame(5, 10, 35, 14, "Item info", "");
        else
            drawFrame(5, 10, 35, 13, "Item info", "");
        paint.setColor(Color.LTGRAY);
        canvas.drawText(capitalize(name), 6 * tileWidth, 12 * tileHeight, paint);
        switch (tag) {
            case 'T' :
            case 'G' : if (modifier != 0)
                           canvas.drawText(String.format("Damage: %d-%d", damage - modifier, damage + modifier), 6 * tileWidth, 13 * tileHeight, paint);
                       else
                           canvas.drawText(String.format("Damage: %d", damage), 6 * tileWidth, 13 * tileHeight, paint);
                       break;
            case 'W' : if (!type.equals("ring"))
                           canvas.drawText(String.format("Armor value: %d (%s)", modifier, type), 6 * tileWidth, 13 * tileHeight, paint);
                       break;
            case 'F' : canvas.drawText(String.format("Blind enemies in room (%d turns)", blindDuration), 6 * tileWidth, 13 * tileHeight, paint);
                       break;
            case 'H' : canvas.drawText("Sends you back to your room", 6 * tileWidth, 13 * tileHeight, paint);
                       break;
        }
        if (tier == 10)
            switch (name) {
                case "Long Bow of the Dark"        : canvas.drawText("Chance to blind enemy", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Hand Cannon"                 : canvas.drawText("It's heavy", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Sword of the Abyss"          : canvas.drawText("Chance to warp enemy away", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Hammer of the Inferno"       : canvas.drawText("Chance of additional damage", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Scythe of the Gravelord"     : canvas.drawText("Chance to reduce enemy's life", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Armor of the Glorious"       : canvas.drawText("Chance to paralyze enemy", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Armor of the Sun"            : canvas.drawText("Chance to get blind on attack", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Armor of Thorns"             : canvas.drawText("Enemy takes damage on attack", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Crown of Dusk"               : canvas.drawText("Chance to blind enemy", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Crown of the Sun"            : canvas.drawText("Chance to miss an attack", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Crown of the Great Lord"     : canvas.drawText("Chance to double the damage", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Helm of Thorns"              : canvas.drawText("Enemy takes damage on attack", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Bracelet of the Great Lord"  : canvas.drawText("Melee damage leeched as life", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Gauntlets of the Vanquisher" : canvas.drawText("Additional bare-handed damage", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Gauntlets of Thorns"         : canvas.drawText("Enemy takes damage on attack", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Boots of the Explorer"       : canvas.drawText("Double warp recharge speed", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Boots of Evasion"            : canvas.drawText("Chance to evade an attack", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Leggings of Thorns"          : canvas.drawText("Enemy takes damage on attack", 6 * tileWidth, 14 * tileHeight, paint); break;
                case "Dragon Ring"                 : canvas.drawText("Boost damage", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Blood Ring"                  : canvas.drawText("Chance to instantly kill enemy", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Hawk Ring"                   : canvas.drawText("Boost ranged weapon damage", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Wolf Ring"                   : canvas.drawText("Boost melee weapon damage", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Calamity Ring"               : canvas.drawText("Double damage taken", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Covetous Ring"               : canvas.drawText("Increase item discovery", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Gold Ring"                   : canvas.drawText("Increase gold acquisition", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Stone Ring"                  : canvas.drawText("Additional armor", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Cling Ring"                  : canvas.drawText("Gain life from fallen enemies", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Ring of Sacrifice"           : canvas.drawText("Prevent lethal damage", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Ancient Ring"                : canvas.drawText("Flask restores additional life", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Quartz Ring"                 : canvas.drawText("Add damage to ranged attacks", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Warrior Ring"                : canvas.drawText("Add damage to melee attacks", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Redeye Ring"                 : canvas.drawText("Wearer is immune to blind", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Silver Ring"                 : canvas.drawText("Wearer is immune to paralyze", 6 * tileWidth, 13 * tileHeight, paint); break;
                case "Vanquisher Ring"             : canvas.drawText("Increase bare-handed damage", 6 * tileWidth, 13 * tileHeight, paint); break;
            }
    }

    private void doWarp(int x1, int y1, int x2, int y2, int cx, int cy) {
        int x, y;

        while (true) {
            boolean done = false;
            x = randomInt(1, maxX - 1);
            y = randomInt(1, maxY - 1);
            if (map[y][x].ch != pass) continue;
            if (x > x1 && x < x2 && y > y1 && y < y2) continue;
            for (Room r : rooms)
                if (x > r.x1 && x < r.x2 && y > r.y1 && y < r.y2) {
                    if (r.x1 == x1 && r.x2 == x2 && r.y1 == y1 && r.y2 == y2) continue;
                    done = true;
                    break;
                }
            if (done) break;
        }
        if (cx == hero.x && cy == hero.y) {
            map[hero.y][hero.x].ch = hero.prevCell;
            flashCell(hero.x, hero.y, Color.GREEN, fxDelay * 5);
            hero.x                 = x;
            hero.y                 = y;
            hero.prevCell          = pass;
            map[hero.y][hero.x].ch = hero.face;
            warpCounter            = 0;
            return;
        }
        for (Player m : monsters)
            if (m.x == cx && m.y == cy) {
                map[m.y][m.x].ch = m.prevCell;
                flashCell(m.x, m.y, Color.GREEN, fxDelay * 5);
                m.x              = x;
                m.y              = y;
                m.prevCell       = pass;
                map[m.y][m.x].ch = m.face;
            }
    }

    private boolean pickUpChest() {
        int     possession   = 0;
        boolean found        = false;
        String  weaponName   = "";
        String  ammoName     = "";
        int     ammoQuantity = 0;
        Item    item;

        for (Item i : items) if (i.x == 0 && !i.name.equals("empty")) possession++;
        if (possession >= inventoryCapacity && !(items.get(selectedItem).tag == 'L' || items.get(selectedItem).tag == 'A')) {
            if (items.get(selectedItem).tag == 'B') {
                for (Ammo a : ammoTypes)
                    if (items.get(selectedItem).name.equals(a.name) && hero.firearm.name.toLowerCase().contains(a.weapon.toLowerCase())) {
                        found = true; break;
                    }
                if (!found)
                    for (Item i : items)
                        if (i.name.equals(items.get(selectedItem).name) && i.x == 0) {
                            found = true; break;
                        }
            }
            if (items.get(selectedItem).tag == 'G') {
                if (items.get(selectedItem).name.equals(hero.firearm.name)) found = true;
                if (!found) {
                    for (Ammo a : ammoTypes)
                        if (items.get(selectedItem).name.toLowerCase().contains(a.weapon.toLowerCase())) {
                            ammoName   = a.name;
                            weaponName = a.weapon;
                            break;
                        }
                    boolean gotWeapon = false, gotAmmo = false;
                    for (Item i : items) {
                        if (i.name.equals(weaponName)) gotWeapon = true;
                        if (i.name.equals(ammoName))   gotAmmo   = true;
                    }
                    if (gotWeapon && gotAmmo) found = true;
                }
            }
            if (!found) {
                displayMessage("Can't carry any more.");
                return false;
            }
            found = false;
        }
        if (possession == inventoryCapacity - 1 && items.get(selectedItem).tag == 'G') {
            if (items.get(selectedItem).name.equals(hero.firearm.name)) found = true;
            if (!found) {
                for (Ammo a : ammoTypes)
                    if (items.get(selectedItem).name.toLowerCase().contains(a.weapon.toLowerCase())) {
                        ammoName   = a.name;
                        weaponName = a.weapon;
                        break;
                    }
                boolean gotAmmo = false;
                for (Item i : items)
                    if (i.name.equals(ammoName)) {
                        gotAmmo = true; break;
                    }
                if (gotAmmo) found = true;
            }
            if (hero.firearm.name.toLowerCase().contains(weaponName.toLowerCase())) found = true;
            if (!found) {
                displayMessage("Can't carry any more.");
                return false;
            }
            found = false;
        }
        switch (items.get(selectedItem).tag) {
            case 'G' : // it's a gun
                       clearMessages();
                       for (Ammo a : ammoTypes)
                           if (items.get(selectedItem).name.toLowerCase().contains(a.weapon.toLowerCase())) {
                               ammoName     = a.name;
                               weaponName   = a.weapon;
                               ammoQuantity = a.quantity;
                               break;
                           }
                       if (hero.firearm.name.equals(items.get(selectedItem).name)) { // I have it equipped
                           hero.ammo += ammoQuantity;
                           found      = true;
                       }
                       if (!found && hero.firearm.name.toLowerCase().contains(weaponName.toLowerCase())) { // compatible weapon equipped
                           hero.ammo  += ammoQuantity;
                           item        = new Item();
                           item.x      = 0;
                           item.y      = 0;
                           item.name   = items.get(selectedItem).name;
                           item.amount = 1;
                           item.tag    = 'G';
                           items.add(item);
                           found = true;
                           displayMessage(String.format("Acquired %s.", item.name));
                       }
                       if (!found) { // let's see
                           for (Item i : items) // do I have it?
                               if (items.get(selectedItem).name.equals(i.name) && i.x == 0) { // yes I do
                                   for (Item j : items)
                                       if (j.name.equals(ammoName) && j.x == 0) {
                                           j.amount += ammoQuantity;
                                           found = true;
                                           break;
                                       }
                                   if (!found) { // no ammo for it though
                                       item        = new Item();
                                       item.x      = 0;
                                       item.y      = 0;
                                       item.name   = ammoName;
                                       item.amount = ammoQuantity;
                                       item.tag    = 'B';
                                       items.add(item);
                                       found = true;
                                   }
                                   break;
                               }
                       }
                       if (found) {
                           displayMessage(String.format("Extracted %d %s.", ammoQuantity, ammoName));
                       }
                       if (!found) { // I don't have it
                           item        = new Item();
                           item.x      = 0;
                           item.y      = 0;
                           item.name   = items.get(selectedItem).name;
                           item.amount = 1;
                           item.tag    = 'G';
                           items.add(item);
                           displayMessage(String.format("Acquired %s.", item.name));
                           for (Item i : items)
                               if (i.name.equals(ammoName) && i.x == 0) { // I have the ammo though
                                   i.amount += ammoQuantity;
                                   found = true; break;
                               }
                           if (!found) { // don't have the ammo either
                               item        = new Item();
                               item.x      = 0;
                               item.y      = 0;
                               item.name   = ammoName;
                               item.amount = ammoQuantity;
                               item.tag    = 'B';
                               items.add(item);
                           }
                           displayMessage(String.format("It's loaded with %d %s.", ammoQuantity, ammoName));
                       }
                       break;
            case 'B' : // ammo
                       for (Ammo a : ammoTypes)
                           if (items.get(selectedItem).name.equals(a.name)) {
                               weaponName = a.weapon;
                               break;
                           }
                       if (hero.firearm.name.toLowerCase().contains(weaponName.toLowerCase())) { // ammo for equipped weapon
                           hero.ammo += items.get(selectedItem).amount;
                           found = true;
                       }
                       if (!found) // check inventory
                           for (Item i : items)
                               if (i.name.equals(items.get(selectedItem).name) && i.x == 0) { // got it
                                   i.amount += items.get(selectedItem).amount;
                                   found = true; break;
                               }
                       if (!found) {
                           item        = new Item();
                           item.x      = 0;
                           item.y      = 0;
                           item.name   = items.get(selectedItem).name;
                           item.amount = items.get(selectedItem).amount;
                           item.tag    = 'B';
                           items.add(item);
                       }
                       clearMessages();
                       displayMessage(String.format("Acquired %d %s.", items.get(selectedItem).amount,
                                                                        items.get(selectedItem).name));
                       break;
            case 'W' : // clothing
            case 'T' : // melee weapon
                       item        = new Item();
                       item.x      = 0;
                       item.y      = 0;
                       item.name   = items.get(selectedItem).name;
                       item.amount = 1;
                       item.tag    = items.get(selectedItem).tag;
                       items.add(item);
                       clearMessages();
                       displayMessage(String.format("Acquired %s.", items.get(selectedItem).name));
                       break;
            case 'H' : // homeward bone
                       for (Item i : items)
                           if (i.name.equals("homeward scroll") && i.x == 0) { // have some
                               i.amount += items.get(selectedItem).amount;
                               found = true; break;
                           }
                       if (!found) {
                           item        = new Item();
                           item.x      = 0;
                           item.y      = 0;
                           item.name   = items.get(selectedItem).name;
                           item.amount = items.get(selectedItem).amount;
                           item.tag    = 'H';
                           items.add(item);
                       }
                       clearMessages();
                       displayMessage(String.format("Acquired %d %s.", items.get(selectedItem).amount,
                                                                       items.get(selectedItem).name));
                       break;
            case 'F' : // flash bomb
                       for (Item i : items)
                           if (i.name.equals("flash bomb") && i.x == 0) { // have some
                               i.amount += items.get(selectedItem).amount;
                               found = true; break;
                           }
                       if (!found) {
                           item        = new Item();
                           item.x      = 0;
                           item.y      = 0;
                           item.name   = items.get(selectedItem).name;
                           item.amount = items.get(selectedItem).amount;
                           item.tag    = 'F';
                           items.add(item);
                       }
                       clearMessages();
                       displayMessage(String.format("Acquired %d %s.", items.get(selectedItem).amount,
                                                                       items.get(selectedItem).name));
                       break;
            case 'L' : // life orb
                       hero.life += orbOfLifeValue;
                       clearMessages();
                       displayMessage(String.format("Gained %d life.", orbOfLifeValue));
                       break;
            case 'A' : // gold cache
                       int boostGold = 0;
                       if (hero.ring1.name.equals("Gold Ring") || hero.ring2.name.equals("Gold Ring")) {
                           boostGold = (int) (goldCacheValue * 0.5f);
                           if (hero.ring1.name.equals("Gold Ring") && hero.ring2.name.equals("Gold Ring"))
                               boostGold = goldCacheValue;
                       }
                       hero.gold += goldCacheValue + boostGold;
                       clearMessages();
                       displayMessage(String.format("Gained %d gold.", goldCacheValue + boostGold));
                       break;
        }
        return true;
    }

    private void autoPickup(int x, int y) {
        int medkitValue   = 0;
        int goldCoinValue = 0;

        for (Misc m : miscStuff) {
            if (m.name.equals("blood vial")) medkitValue   = m.value;
            if (m.name.equals("gold coin"))  goldCoinValue = m.value;
        }
        for (Item i : items)
            if (i.x == x && i.y == y && i.tag == 'M' && !i.name.equals("empty")) {
                switch (i.name) {
                    case "blood vial" :
                        hero.life += medkitValue;
                        i.name = "empty";
                        displayMessage(String.format("Picked up blood vial. (+%d life)", medkitValue));
                        break;
                    case "gold coin" :
                        int boostGold = 0;
                        if (hero.ring1.name.equals("Gold Ring") || hero.ring2.name.equals("Gold Ring")) {
                            boostGold = (int) (goldCoinValue * 0.5f);
                            if (hero.ring1.name.equals("Gold Ring") && hero.ring2.name.equals("Gold Ring"))
                                boostGold = goldCoinValue;
                        }
                        hero.gold += goldCoinValue + boostGold;
                        i.name = "empty";
                        displayMessage(String.format("Picked up %d gold.", goldCoinValue + boostGold));
                        break;
                    case "keystone" :
                        i.x = 0;
                        i.y = 0;
                        displayMessage("Picked up keystone.");
                }
            }
        cleanUpItems(hero.x, hero.y);
    }

    private void blindMonsters() {
        final Point[] theRoom = new Point[2];

        theRoom[0] = new Point(); theRoom[1] = new Point();
        theRoom[0].x = 0; theRoom[0].y = 0;
        theRoom[1].x = 0; theRoom[1].y = 0;
        for (Room r : rooms)
            if (hero.x > r.x1 && hero.x < r.x2 && hero.y > r.y1 && hero.y < r.y2) {
                theRoom[0].x = r.x1 + 1; theRoom[0].y = r.y1 + 1;
                theRoom[1].x = r.x2 - 1; theRoom[1].y = r.y2 - 1;
                break;
            }
        if (theRoom[0].x == 0) { // used in a corridor = only works for adjacent cells
            for (int i = hero.y - 1; i <= hero.y + 1; i++)
                for (int j = hero.x - 1; j <= hero.x + 1; j++)
                    for (Player m : monsters) {
                        if (m.x == j && m.y == i && m.state) m.blindTimer = 0;
                        if (map[i][j].ch != wall) clearCell(j, i, Color.WHITE);
                    }
        }
        if (theRoom[0].x > 0) {
            for (Player m : monsters)
                if (m.x >= theRoom[0].x && m.x <= theRoom[1].x && m.y >= theRoom[0].y && m.y <= theRoom[1].y && m.state)
                    m.blindTimer = 0;
            new Thread() {
                public void run() {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            for (int i = theRoom[0].y; i <= theRoom[1].y; i++)
                                for (int j = theRoom[0].x; j <= theRoom[1].x; j++)
                                    clearCell(j, i, Color.WHITE);
                            imageView.invalidate();
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    drawMap();
                                    imageView.invalidate();
                                }
                            }, fxDelay * 5);
                            long    now = System.currentTimeMillis();
                            long future = now + fxDelay * 5;
                            while (future > now) now = System.currentTimeMillis();
                        }
                    });
                }
            }.start();
        }
    }

    private void movePlayer() {
        int mX = 0, mY = 0;

        switch (direction) {
            case 4 : mX--; break;
            case 6 : mX++; break;
            case 8 : mY--; break;
            case 2 : mY++; break;
            case 1 : mX--; mY++; break;
            case 7 : mX--; mY--; break;
            case 3 : mX++; mY++; break;
            case 9 : mX++; mY--; break;
        }
        if (map[hero.y + mY][hero.x + mX].ch == pass) {
            map[hero.y][hero.x].ch = hero.prevCell;
            hero.x += mX;
            hero.y += mY;
            hero.prevCell = pass;
            map[hero.y][hero.x].ch = hero.face;
            drawMap();
            return;
        }
        if (map[hero.y + mY][hero.x + mX].ch == door) {
            if (depth == maxDepth) {
                for (Item i : items)
                    if (i.name.equals("keystone") && i.x == 0) {
                        depth     = 0;
                        direction = 0;
                        townInstance();
                        displayMessage("You've been sent back.");
                        return;
                    }
                    displayMessage("The fog is too thick.");
                    return;
            }
            depth++;
            direction = 0;
            newInstance();
            return;
        }
        if (map[hero.y + mY][hero.x + mX].ch == gate) {
            winCondition = true;
            displayMessage("You have escaped!");
            return;
        }
        if (map[hero.y + mY][hero.x + mX].ch == chest) {
            int i = 0;
            displayMessage("There is a chest.");
            for (int j = 0; j < items.size(); j++)
                if (items.get(j).x == hero.x + mX && items.get(j).y == hero.y + mY) {
                    i = j; break;
                }
            if (items.get(i).name.equals("empty")) {
                displayMessage("It's empty.");
                return;
            }
            if (hero.blindTimer < blindDuration) {
                displayMessage("You can't tell what's inside.");
                return;
            }
            if (items.get(i).name.equals("unknown")) generateItem(i, 'c');
            displayMessage(String.format("You see %d %s inside.", items.get(i).amount, items.get(i).name));
            displayMessage("Would you like to pick that up?");
            gameState    = 'C';
            selectedItem = i;
            direction    = 0;
            drawConfirmationButtons();
            return;
        }
        if (map[hero.y + mY][hero.x + mX].ch == stone) {
            if (hero.blindTimer < blindDuration) {
                displayMessage("Bump.");
                return;
            }
            boolean found = false;
            for (Item i : items)
                if (i.name.equals("keystone") && i.x == 0) {
                    displayMessage("The gate is open.");
                    rooms.add(new Room(maxX / 3 + 1, maxY - (maxY / 3) * 2, (maxX / 3) * 2 - 1, maxY - maxY / 3 - 1, true));
                    addRoom(maxX / 3 + 1, maxY - (maxY / 3) * 2, (maxX / 3) * 2 - 1, maxY - maxY / 3 - 1);
                    int pX = maxX / 2, pY = maxY - 4;
                    map[pY + 1][pX].ch     = pass;
                    map[pY + 1][pX + 1].ch = pass;
                    while (map[pY][pX].ch != wall) {
                        map[pY][pX - 1].ch = wall;
                        map[pY][pX].ch     = pass;
                        map[pY][pX + 1].ch = pass;
                        map[pY][pX + 2].ch = wall;
                        pY--;
                    }
                    map[pY][pX].ch     = pass;
                    map[pY][pX + 1].ch = pass;
                    while (map[pY][pX].ch == pass) pY--;
                    map[pY][pX].ch     = gate;
                    map[pY][pX + 1].ch = gate;
                    found = true;
                    drawMap();
                    break;
                }
            if (!found) {
                displayMessage("\"Be wary of imminent despair.\"");
                displayMessage("You feel uncomfortable for a moment.");
            }
            return;
        }
        if (map[hero.y + mY][hero.x + mX].ch == stash) {
            if (hero.blindTimer < blindDuration) {
                displayMessage("Bump.");
                return;
            }
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.info),    screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.move),    screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel),  screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
            stashItems(1);
            imageView.invalidate();
            direction = 0;
            return;
        }
        if (map[hero.y + mY][hero.x + mX].ch == waypoint) {
            if (hero.blindTimer < blindDuration) return;
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.confirm), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel),  screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
            drawFrame(13, 7, 27, 18, "Destination", "");
            for (int i = 1; i <= progression; i++)
                if (i % 10 == 1) {
                    paint.setColor(Color.WHITE);
                    canvas.drawText(Integer.toString(i), 16 * tileWidth, (9 + i / 10) * tileHeight, paint);
                }
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pointer), 15 * tileWidth, 8 * tileHeight + 8, null);
            gameState = 'X';
            direction = 0;
            return;
        }
        if (map[hero.y + mY][hero.x + mX].ch == vendor) {
            if (hero.blindTimer < blindDuration) {
                displayMessage("\"You alright?\"");
                return;
            }
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.info),    screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.buy),     screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
            canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel),  screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
            displayMessage("Shopkeeper greets you.");
            displayMessage("\"Take a look at my fine wares.\"");
            Item item   = new Item();
            item.x      = 101;
            item.y      = 101;
            item.name   = "flash bomb";
            item.tag    = 'F';
            item.amount = 1;
            item.price  = 200;
            mine.add(item);
            item        = new Item();
            item.x      = 101;
            item.y      = 101;
            item.name   = "homeward scroll";
            item.tag    = 'H';
            item.amount = 1;
            item.price  = 300;
            mine.add(item);
            item        = new Item();
            item.x      = 101;
            item.y      = 101;
            item.name   = "life flask refill";
            item.tag    = 'R';
            item.amount = 1;
            item.price  = 100;
            mine.add(item);
            for (Ammo a : ammoTypes) {
                item        = new Item();
                item.x      = 101;
                item.y      = 101;
                item.name   = a.name;
                item.amount = a.quantity;
                item.tag    = 'B';
                item.price  = a.price;
                if (!a.name.equals("empty")) mine.add(item);
            }
            listItems("Things for sale");
            gameState = 'V';
            direction = 0;
            return;
        }
        if (map[hero.y + mY][hero.x + mX].ch == clip) {
            map[hero.y][hero.x].ch = hero.prevCell;
            hero.x += mX;
            hero.y += mY;
            map[hero.y][hero.x].ch = hero.face;
            drawMap();
            direction = 0;
            if (hero.blindTimer < blindDuration) {
                displayMessage("Ouch.");
                return;
            }
            checkTheGround();
            return;
        }
        if (map[hero.y + mY][hero.x + mX].ch == corpse) {
            map[hero.y][hero.x].ch = hero.prevCell;
            hero.x += mX;
            hero.y += mY;
            map[hero.y][hero.x].ch = hero.face;
            hero.prevCell = corpse;
            drawMap();
            autoPickup(hero.x, hero.y);
            if (hero.blindTimer < blindDuration) {
                displayMessage("Ouch.");
                return;
            }
            for (Item i : items)
                if (i.x == hero.x && i.y == hero.y && !i.name.equals("empty")) {
                    direction = 0;
                    checkTheGround();
                    break;
                }
            return;
        }
        if (Character.isLetter(map[hero.y + mY][hero.x + mX].ch)) {
            if (doDamage(hero.x + mX, hero.y + mY, 'm')) {
                map[hero.y + mY][hero.x + mX].ch = corpse;
                drawMap();
            }
        }
    }

    private void moveMonsters() {
        Point[] theRoom = new Point[2];

        theRoom[0] = new Point(); theRoom[1] = new Point();
        theRoom[0].x = -1; theRoom[0].y = -1;
        theRoom[1].x = -1; theRoom[1].y = -1;
        for (Room r : rooms)
            if (hero.x > r.x1 && hero.x < r.x2 && hero.y > r.y1 && hero.y < r.y2) {
                theRoom[0].x = r.x1 + 1; theRoom[0].y = r.y1 + 1;
                theRoom[1].x = r.x2 - 1; theRoom[1].y = r.y2 - 1;
                break;
            }
        for (Player m : monsters) {
            if (!m.state) continue;
            boolean blind = false;
            if (m.paralyzeTimer < paralyzeDuration) {
                m.paralyzeTimer++;
                continue;
            }
            if (m.blindTimer < blindDuration) {
                m.blindTimer++;
                blind = true;
            }
            boolean done = false;
            if (((m.x >= theRoom[0].x && m.x <= theRoom[1].x &&
                  m.y >= theRoom[0].y && m.y <= theRoom[1].y) || m.pursuit) && !blind) { // aggro
                if (!m.pursuit) {
                    m.pursuit = true;
                    if (m.name.equals(monsterTypes.get(monsterTypes.size() - 1).name))
                        displayMessage("\"I shall end your suffering!\"");
                    continue;
                }
                if (m.face == 'L') { // demon lord
                    int rng = randomInt(0, 99);
                    if (rng < 49) {
                        for (int y = hero.y - 1; y <= hero.y + 1; y++)
                            for (int x = hero.x - 1; x <= hero.x + 1; x++)
                                if (map[y][x].ch == pass || map[y][x].ch == corpse || map[y][x].ch == clip) {
                                    flashCell(m.x, m.y, Color.GREEN, fxDelay * 5);
                                    map[m.y][m.x].ch = m.prevCell;
                                    m.prevCell       = map[y][x].ch;
                                    map[y][x].ch     = m.face;
                                    m.x              = x;
                                    m.y              = y;
                                }
                    }
                }
                boolean miss = false;
                if (hero.legs.name.equals("Boots of Evasion")) {
                    int rng = randomInt(0, 99);
                    if (rng < 20) miss = true;
                }
                for (int i = 1; i < gunTypes.size(); i++) // guncheck
                    if (m.firearm.name.equals(gunTypes.get(i).name)) { // monster armed with a gun
                        if (connectXY(m.x, m.y, hero.x, hero.y, 'c')) { // clear shot
                            int damage = monsterDamage(m, 'r');
                            if (damage < 0) damage = 0;
                            displayMessage(String.format("%s shoots you for %d damage.", capitalize(m.name), damage));
                            if (miss) {
                                displayMessage("You evade!");
                                done = true;
                                break;
                            }
                            hero.prevLife = hero.life;
                            hero.life    -= damage;
                            flashCell(m.x, m.y, Color.RED, fxDelay * 4);
                            if (hero.life <= 0) {
                                displayMessage("You die...");
                                return;
                            }
                            if (m.face == 'k') { // skeleton mage
                                int rng = randomInt(0, 99);
                                if (rng < 25) {
                                    if (!(hero.ring1.name.equals("Silver Ring") || hero.ring2.name.equals("Silver Ring"))) {
                                        if (hero.paralyzeTimer >= paralyzeDuration) hero.paralyzeTimer = 0;
                                        displayMessage("You are paralyzed!");
                                    }
                                }
                            }
                            if (m.face == 'W') { // witch
                                int rng = randomInt(0, 99);
                                if (rng < 25) {
                                    if (!(hero.ring1.name.equals("Redeye Ring") || hero.ring2.name.equals("Redeye Ring"))) {
                                        if (hero.blindTimer >= blindDuration) hero.blindTimer = 0;
                                        displayMessage("You are blinded!");
                                    }
                                }
                            }
                            done = true;
                        }
                        break;
                    }
                if (done) continue;
                for (int i = 1; i < toolTypes.size(); i++) // melee check
                    if (m.tool.name.equals(toolTypes.get(i).name)) { // armed with a melee weapon
                        for (int y = m.y - 1; y <= m.y + 1; y++)
                            for (int x = m.x - 1; x <= m.x + 1; x++)
                                if (map[y][x].ch == hero.face) { // connected
                                    int damage = monsterDamage(m, 'm');
                                    if (damage < 0) damage = 0;
                                    displayMessage(String.format("%s hits you for %d damage.", capitalize(m.name), damage));
                                    if (miss) {
                                        displayMessage("You evade!");
                                        done = true;
                                        break;
                                    }
                                    hero.prevLife = hero.life;
                                    hero.life    -= damage;
                                    flashCell(m.x, m.y, Color.RED, fxDelay * 4);
                                    if (hero.life <= 0) {
                                        displayMessage("You die...");
                                        return;
                                    }
                                    if (m.face == 'v') { // vampire
                                        flashCell(m.x, m.y, Color.BLUE, fxDelay * 4);
                                        m.life += damage * 0.3f;
                                    }
                                    if (m.face == 'G') { // stone dragon
                                        int rng = randomInt(0, 99);
                                        if (rng < 25) {
                                            if (!(hero.ring1.name.equals("Silver Ring") || hero.ring2.name.equals("Silver Ring"))) {
                                                if (hero.paralyzeTimer >= paralyzeDuration) hero.paralyzeTimer = 0;
                                                displayMessage("You are paralyzed!");
                                            }
                                        }
                                    }
                                    if (m.face == 'E') { // bone dragon
                                        int rng = randomInt(0, 99);
                                        if (rng < 25) {
                                            if (!(hero.ring1.name.equals("Redeye Ring") || hero.ring2.name.equals("Redeye Ring"))) {
                                                if (hero.blindTimer >= blindDuration) hero.blindTimer = 0;
                                                displayMessage("You are blinded!");
                                            }
                                        }
                                    }
                                    doDamage(m.x, m.y, 'c'); // thorns
                                    done = true;
                                    break;
                                }
                        break;
                    }
                if (done) continue;
                if (connectXY(m.x, m.y, hero.x, hero.y, 'm'))
                    connectXY(m.x, m.y, hero.x, hero.y, 'p');
                else
                    pathfind(m.x, m.y, hero.x, hero.y);
                if (map[nextY][nextX].ch == pass || map[nextY][nextX].ch == corpse || map[nextY][nextX].ch == clip) { // move towards player
                    map[m.y][m.x].ch     = m.prevCell;
                    m.prevCell           = map[nextY][nextX].ch;
                    map[nextY][nextX].ch = m.face;
                    m.x                  = nextX;
                    m.y                  = nextY;
                }
                done = true;
            }
            if (done) continue;
            int direction = randomInt(1, 9); // prowler
            int x = 0, y = 0;
            switch (direction) {
                case 4 : x--; break;
                case 6 : x++; break;
                case 8 : y--; break;
                case 2 : y++; break;
                case 1 : x--; y++; break;
                case 7 : x--; y--; break;
                case 3 : x++; y++; break;
                case 9 : x++; y--;
            }
            if (map[m.y + y][m.x + x].ch == pass || map[m.y + y][m.x + x].ch == corpse || map[m.y + y][m.x + x].ch == clip) {
                map[m.y][m.x].ch         = m.prevCell;
                m.prevCell               = map[m.y + y][m.x + x].ch;
                map[m.y + y][m.x + x].ch = m.face;
                m.x                     += x;
                m.y                     += y;
            }
        }
    }

    private void pathfind(int x1, int y1, int x2, int y2) {
        List<int[]> queue  = new ArrayList<int[]>();
        boolean     done   = false;
        boolean     stuck  = false;
        boolean     reInit = false;
        int         steps  = 0;

        queue.add(new int[3]);
        queue.get(0)[0] = x2; queue.get(0)[1] = y2; queue.get(0)[2] = 0;
        while (!done) {
            int     queueLength = queue.size();
            boolean added       = false;
            for (int i = 0; i < queueLength; i++) {
                if (queue.get(i)[2] < steps) continue;
                int x = queue.get(i)[0];
                int y = queue.get(i)[1];
                for (int j = y - 1; j <= y + 1; j++)
                    for (int k = x - 1; k <= x + 1; k++) {
                        if (k == x1 && j == y1) { done = true; break; }
                        if (!stuck && ((j == y && k == x) ||
                            map[j][k].ch == wall || map[j][k].ch == chest || map[j][k].ch == door ||
                            Character.isLetter(map[j][k].ch) || inQueue(k, j, queue))) continue;
                        if (stuck && ((j == y && k == x) ||
                            map[j][k].ch == wall || map[j][k].ch == chest || map[j][k].ch == door ||
                            inQueue(k, j, queue))) continue;
                        added = true;
                        queue.add(new int[3]);
                        queue.get(queue.size() - 1)[0] = k;
                        queue.get(queue.size() - 1)[1] = j;
                        queue.get(queue.size() - 1)[2] = steps + 1;
                    }
                if (done) break;
            }
            if (!added && !done) stuck = true;
            if (stuck && !reInit) {
                reInit = true; steps = -1;
                queue.clear();
                queue.add(new int[3]);
                queue.get(0)[0] = x2; queue.get(0)[1] = y2; queue.get(0)[2] = 0;
            }
            steps++;
        }
        nextX = x1; nextY = y1;
        for (int i = 0; i < queue.size(); i++)
            if (queue.get(i)[2] == steps - 1 &&
                queue.get(i)[0] >= x1 - 1 && queue.get(i)[0] <= x1 + 1 &&
                queue.get(i)[1] >= y1 - 1 && queue.get(i)[1] <= y1 + 1) {
                nextX = queue.get(i)[0];
                nextY = queue.get(i)[1];
                break;
            }
    }

    private boolean inQueue(int x, int y, List<int[]> queue) {
        for (int[] q : queue)
            if (q[0] == x && q[1] == y) return true;
        return false;
    }

    private void checkTheGround() {
        displayMessage("You see something on the ground.");
        displayMessage("Do you want to check it out?");
        gameState = 'Y';
        drawConfirmationButtons();
    }

    private boolean doDamage(int x, int y, char damageSource) {
        int modRng, modValue, damageValue = 0;
        int i = -1;

        if (damageSource != 'c') clearMessages();
        if (damageSource == 'r' && hero.ammo < 1) {
            displayMessage("No ammo left.");
            return false;
        }
        if (damageSource == 'r' && hero.firearm.name.equals("empty")) return false;
        for (int j = 0; j < monsters.size(); j++)
            if (monsters.get(j).x == x && monsters.get(j).y == y && monsters.get(j).state) {
                i = j; break;
            }
        modRng = randomInt(0, 99);
        int damageBoost = 0;
        switch (damageSource) {
            case 'm' : // melee
                       modValue = hero.tool.modifier == 0 ? 0 : randomInt(0, hero.tool.modifier);
                       if (modRng > 49)
                           damageValue = hero.tool.damage + modValue;
                       else
                           damageValue = hero.tool.damage - modValue;
                       if (hero.tool.name.equals("Hammer of the Inferno")) {
                           int rng = randomInt(0, 99);
                           if (rng < 25) damageValue += randomInt(8, 12);
                       }
                       if (hero.ring1.name.equals("Warrior Ring")) damageValue += randomInt(5, 7);
                       if (hero.ring2.name.equals("Warrior Ring")) damageValue += randomInt(5, 7);
                       if (hero.tool.name.equals("fists") && hero.glove.name.equals("Gauntlets of the Vanquisher")) damageValue += randomInt(8, 10);
                       if (hero.ring1.name.equals("Wolf Ring")) damageBoost += damageValue * 0.25f;
                       if (hero.ring2.name.equals("Wolf Ring")) damageBoost += damageValue * 0.25f;
                       damageValue += damageBoost;
                       damageBoost = 0;
                       if (hero.tool.name.equals("fists") && hero.ring1.name.equals("Vanquisher Ring")) damageBoost += damageValue * 5;
                       if (hero.tool.name.equals("fists") && hero.ring2.name.equals("Vanquisher Ring")) damageBoost += damageValue * 5;
                       damageValue += damageBoost;
                       break;
            case 'r' : // ranged
                       modValue = hero.firearm.modifier == 0 ? 0 : randomInt(0, hero.firearm.modifier);
                       if (modRng > 49)
                           damageValue = hero.firearm.damage + modValue;
                       else
                           damageValue = hero.firearm.damage - modValue;
                       damageBoost = 0;
                       if (hero.ring1.name.equals("Quartz Ring")) damageValue += randomInt(5, 7);
                       if (hero.ring2.name.equals("Quartz Ring")) damageValue += randomInt(5, 7);
                       if (hero.ring1.name.equals("Hawk Ring")) damageBoost += damageValue * 0.25f;
                       if (hero.ring2.name.equals("Hawk Ring")) damageBoost += damageValue * 0.25f;
                       damageValue += damageBoost;
                       break;
            case 'c' : // thorns
                       if (hero.chest.name.equals("Armor of Thorns"))     damageValue += randomInt(10, 12);
                       if (hero.head.name.equals("Helm of Thorns"))       damageValue += randomInt( 3,  4);
                       if (hero.glove.name.equals("Gauntlets of Thorns")) damageValue += randomInt( 5,  8);
                       if (hero.legs.name.equals("Leggings of Thorns"))   damageValue += randomInt( 2,  3);
                       if (damageValue == 0) return false;
                       break;
        }
        flashCell(hero.x, hero.y, Color.RED, fxDelay * 4);
        if (damageSource == 'r') hero.ammo--;
        if (damageSource == 'r' && hero.firearm.name.equals("double rifle") && hero.ammo > 0) hero.ammo--;
        if (x == hero.x && y == hero.y) {
            damageValue -= hero.armor / arMitigation;
            if (damageValue < 0) damageValue = 0;
            hero.prevLife = hero.life;
            hero.life    -= damageValue;
            displayMessage(String.format("You deal %d damage to yourself.", damageValue));
            if (hero.life <= 0) displayMessage("You die...");
            return false;
        }
        if (i < 0) {
            displayMessage("Pew.");
            return false;
        }
        if (monsters.get(i).face == 'X' && damageSource != 'c') { // Fest
            int rng = randomInt(0, 99);
            if (rng < 20) {
                damageValue -= hero.armor / arMitigation;
                if (damageValue < 0) damageValue = 0;
                hero.prevLife = hero.life;
                hero.life    -= damageValue;
                displayMessage(String.format("Reflect! You take %d damage.", damageValue));
                if (hero.life <= 0) displayMessage("You die...");
                return false;
            }
        }
        damageBoost = 0;
        if (hero.ring1.name.equals("Dragon Ring") && damageSource != 'c') damageBoost += damageValue * 0.2f;
        if (hero.ring2.name.equals("Dragon Ring") && damageSource != 'c') damageBoost += damageValue * 0.2f;
        damageValue += damageBoost;
        if (hero.head.name.equals("Crown of the Great Lord") && damageSource != 'c') {
            int rng = randomInt(0, 99);
            if (rng < 10) damageValue *= 2;
        }
        damageValue -= monsters.get(i).armor / arMitigation;
        if (damageValue < 0) damageValue = 0;
        boolean miss = false;
        if (monsters.get(i).face == 'S' && damageSource != 'c') { // succubus
            int rng = randomInt(0, 99);
            if (rng < 30) {
                miss = true;
                displayMessage("You miss!");
            }
        }
        if (hero.head.name.equals("Crown of the Sun")  && damageSource != 'c' && !miss) {
            int rng = randomInt(0, 99);
            if (rng < 10) {
                miss = true;
                displayMessage("You miss!");
            }
        }
        if (!miss) {
            monsters.get(i).life -= damageValue;
            if (damageSource == 'c')
                displayMessage(String.format("%s takes %d damage.", capitalize(monsters.get(i).name), damageValue));
            else {
                displayMessage(String.format("You deal %d damage to %s.", damageValue, monsters.get(i).name));
                if (hero.glove.name.equals("Bracelet of the Great Lord")) {
                    hero.life += damageValue / 5;
                }
                if (hero.ring1.name.equals("Blood Ring")) {
                    int rng = randomInt(0, 99);
                    if (rng < 10) monsters.get(i).life = 0;
                }
                if (hero.ring2.name.equals("Blood Ring")) {
                    int rng = randomInt(0, 99);
                    if (rng < 10) monsters.get(i).life = 0;
                }
            }
        }
        monsters.get(i).pursuit = true;
        if (monsters.get(i).life <= 0) {
            monsters.get(i).face  = corpse;
            monsters.get(i).state = false;
            displayMessage(String.format("%s dies.", capitalize(monsters.get(i).name)));
            if (damageSource == 'c') {
                map[monsters.get(i).y][monsters.get(i).x].ch = corpse;
                drawMap();
            }
            if (hero.ring1.name.equals("Cling Ring")) hero.life += 5;
            if (hero.ring2.name.equals("Cling Ring")) hero.life += 5;
            if (hero.ring1.name.equals("Covetous Ring") || hero.ring2.name.equals("Covetous Ring")) {
                int rngBoost = 1;
                if (hero.ring1.name.equals("Covetous Ring") && hero.ring2.name.equals("Covetous Ring"))
                    rngBoost = 2;
                modRng  = randomInt(1, miscStuff.size() + rngBoost);
            } else
                modRng  = randomInt(1, miscStuff.size());
            Item item   = new Item();
            item.x      = monsters.get(i).x;
            item.y      = monsters.get(i).y;
            if (modRng < miscStuff.size()) {
                item.name   = miscStuff.get(modRng).name;
                item.amount = miscStuff.get(modRng).value;
                item.tag    = 'M';
            }
            items.add(item);
            if (modRng >= miscStuff.size()) generateItem(items.size() - 1, 'm');
            if (monsters.get(i).name.equals(monsterTypes.get(monsterTypes.size() - 1).name)) {
                item        = new Item();
                item.x      = monsters.get(i).x;
                item.y      = monsters.get(i).y;
                item.name   = "keystone";
                item.amount = 1;
                item.tag    = 'M';
                items.add(item);
            }
            return true;
        } else {
            if (damageSource == 'c') return false;
            if (hero.chest.name.equals("Armor of the Glorious")) {
                int rng = randomInt(0, 99);
                if (rng < 10) {
                    monsters.get(i).paralyzeTimer = 0;
                    displayMessage(String.format("%s is paralyzed!", capitalize(monsters.get(i).name)));
                }
            }
            if (hero.chest.name.equals("Armor of the Sun")) {
                int rng = randomInt(0, 99);
                if (rng < 10) {
                    if (!(hero.ring1.name.equals("Redeye Ring") || hero.ring2.name.equals("Redeye Ring"))) {
                        if (hero.blindTimer >= blindDuration) hero.blindTimer = 0;
                        displayMessage("The light shines!");
                    }
                }
            }
            if (hero.head.name.equals("Crown of Dusk")) {
                int rng = randomInt(0, 99);
                if (rng < 10) {
                    monsters.get(i).blindTimer = 0;
                    displayMessage(String.format("%s is blinded!", capitalize(monsters.get(i).name)));
                }
            }
            if (hero.tool.name.equals("Sword of the Abyss") && damageSource == 'm') {
                int rng = randomInt(0, 99);
                if (rng < 15) {
                    boolean didWarp = false;
                    for (Room r : rooms)
                        if (monsters.get(i).x > r.x1 && monsters.get(i).y < r.x2 &&
                            monsters.get(i).y > r.y1 && monsters.get(i).y < r.y2) {
                            doWarp(r.x1, r.y1, r.x2, r.y2, monsters.get(i).x, monsters.get(i).y);
                            didWarp = true;
                            break;
                        }
                    if (!didWarp) doWarp(monsters.get(i).x, monsters.get(i).y, monsters.get(i).x, monsters.get(i).y, monsters.get(i).x, monsters.get(i).y);
                }
            }
            if (hero.tool.name.equals("Scythe of the Gravelord") && damageSource == 'm') {
                int rng = randomInt(0, 99);
                if (rng < 20) {
                    monsters.get(i).life -= monsters.get(i).life * 0.3f;
                    if (monsters.get(i).life <= 0) monsters.get(i).life = 1;
                    displayMessage(String.format("%s shivers!", capitalize(monsters.get(i).name)));
                }
            }
            if (hero.tool.name.equals("Long Bow of the Dark") && damageSource == 'r') {
                int rng = randomInt(0, 99);
                if (rng < 10) {
                    monsters.get(i).blindTimer = 0;
                    displayMessage(String.format("%s is blinded!", capitalize(monsters.get(i).name)));
                }
            }
        }
        return false;
    }

    private int monsterDamage(Player monster, char damageSource) {
        int damage = 0, modifier = 0;

        switch (damageSource) {
            case 'r' : // ranged attack
                       for (Weapon w : gunTypes)
                           if (monster.firearm.name.equals(w.name)) {
                               damage   = w.damage;
                               modifier = w.modifier;
                               break;
                           }
                       break;
            case 'm' : // melee hit
                       for (Weapon w : toolTypes)
                           if (monster.tool.name.equals(w.name)) {
                               damage   = w.damage;
                               modifier = w.modifier;
                               if (monster.face == 'D') { // swamp dragon
                                   int rng = randomInt(0, 99);
                                   if (rng < 20) {
                                       damage   += 17;
                                       modifier +=  1;
                                       displayMessage("Acid breath!");
                                   }
                               }
                               break;
                           }
        }
        int modRng      = randomInt(0, 99);
        int modValue    = modifier == 0 ? 0 : randomInt(0, modifier);
        int damageValue = modRng < 49 ? damage - modValue : damage + modValue;
        if (hero.ring1.name.equals("Calamity Ring") || hero.ring2.name.equals("Calamity Ring")) {
            int calamityDamage = (damageValue - (hero.armor / arMitigation)) * 2;
            if (hero.ring1.name.equals("Calamity Ring") && hero.ring2.name.equals("Calamity Ring"))
                calamityDamage = (damageValue - (hero.armor / arMitigation)) * 4;
            return calamityDamage;
        } else
            return damageValue - (hero.armor / arMitigation);
    }

    private void generateItem(int i, char mode) {
        int rng, moRng, itemTier = 0;

        if (depth <= 10)                itemTier =  1;
        if (depth >  10 && depth <= 20) itemTier =  2;
        if (depth >  20 && depth <= 30) itemTier =  3;
        if (depth >  30 && depth <= 40) itemTier =  4;
        if (depth >  40 && depth <= 50) itemTier =  5;
        if (depth >  50 && depth <= 60) itemTier =  6;
        if (depth >  60 && depth <= 70) itemTier =  7;
        if (depth >  70 && depth <= 80) itemTier =  8;
        if (depth >  80 && depth <= 90) itemTier =  9;
        rng = randomInt(0, 453);
        while (mode == 'm' && (rng >= 200 && rng <= 399)) rng = randomInt(0, 453);
        if (rng >= 0 && rng <= 49) { // ranged weapon
            moRng               = randomInt(1, gunTypes.size() - 1);
            while (gunTypes.get(moRng).tier > itemTier) moRng = randomInt(1, gunTypes.size() - 1);
            items.get(i).name   = gunTypes.get(moRng).name;
            items.get(i).amount = 1;
            items.get(i).tag    = 'G';
        }
        if (rng >= 50 && rng <= 99) { // melee weapon
            moRng               = randomInt(2, toolTypes.size() - 1);
            while (toolTypes.get(moRng).tier > itemTier) moRng = randomInt(2, toolTypes.size() - 1);
            items.get(i).name   = toolTypes.get(moRng).name;
            items.get(i).amount = 1;
            items.get(i).tag    = 'T';
        }
        if (rng >= 100 && rng <= 199) { // ammo
            moRng               = randomInt(1, ammoTypes.size() - 1);
            items.get(i).name   = ammoTypes.get(moRng).name;
            items.get(i).amount = ammoTypes.get(moRng).quantity;
            items.get(i).tag    = 'B';
        }
        if (rng >= 200 && rng <= 299) { // orb of life
            items.get(i).name   = "orb of life";
            items.get(i).amount = 1;
            items.get(i).tag    = 'L';
        }
        if (rng >= 300 && rng <= 399) { // gold cache
            items.get(i).name   = "gold cache";
            items.get(i).amount = 1;
            items.get(i).tag    = 'A';
        }
        if (rng >= 400 && rng <= 449) { // clothes
            moRng               = randomInt(1, equipTypes.size() - 1);
            while (equipTypes.get(moRng).tier > itemTier) moRng = randomInt(1, equipTypes.size() - 1);
            items.get(i).name   = equipTypes.get(moRng).name;
            items.get(i).amount = 1;
            items.get(i).tag    = 'W';
        }
        if (rng >= 450 && rng <= 451) { // rings
            moRng               = randomInt(1, equipTypes.size() - 1);
            while (!equipTypes.get(moRng).type.equals("ring")) moRng = randomInt(1, equipTypes.size() - 1);
            items.get(i).name   = equipTypes.get(moRng).name;
            items.get(i).amount = 1;
            items.get(i).tag    = 'W';
        }
        if (rng == 452) { // unique shit
            rng = randomInt(0, 5);
            if (rng == 0) { // unique head
                moRng               = randomInt(1, equipTypes.size() - 1);
                while (!equipTypes.get(moRng).type.equals("head") && equipTypes.get(moRng).tier != 10) moRng = randomInt(1, equipTypes.size() - 1);
                items.get(i).name   = equipTypes.get(moRng).name;
                items.get(i).amount = 1;
                items.get(i).tag    = 'W';
            }
            if (rng == 1) { // unique chest
                moRng               = randomInt(1, equipTypes.size() - 1);
                while (!equipTypes.get(moRng).type.equals("chest") && equipTypes.get(moRng).tier != 10) moRng = randomInt(1, equipTypes.size() - 1);
                items.get(i).name   = equipTypes.get(moRng).name;
                items.get(i).amount = 1;
                items.get(i).tag    = 'W';
            }
            if (rng == 2) { // unique gloves
                moRng               = randomInt(1, equipTypes.size() - 1);
                while (!equipTypes.get(moRng).type.equals("glove") && equipTypes.get(moRng).tier != 10) moRng = randomInt(1, equipTypes.size() - 1);
                items.get(i).name   = equipTypes.get(moRng).name;
                items.get(i).amount = 1;
                items.get(i).tag    = 'W';
            }
            if (rng == 3) { // unique boots
                moRng               = randomInt(1, equipTypes.size() - 1);
                while (!equipTypes.get(moRng).type.equals("legs") && equipTypes.get(moRng).tier != 10) moRng = randomInt(1, equipTypes.size() - 1);
                items.get(i).name   = equipTypes.get(moRng).name;
                items.get(i).amount = 1;
                items.get(i).tag    = 'W';
            }
            if (rng == 4) { // unique ranged weapon
                moRng               = randomInt(1, gunTypes.size() - 1);
                while (gunTypes.get(moRng).tier != 10) moRng = randomInt(1, gunTypes.size() - 1);
                items.get(i).name   = gunTypes.get(moRng).name;
                items.get(i).amount = 1;
                items.get(i).tag    = 'G';
            }
            if (rng == 5) { // unique melee weapon
                moRng               = randomInt(2, toolTypes.size() - 1);
                while (toolTypes.get(moRng).tier != 10) moRng = randomInt(2, toolTypes.size() - 1);
                items.get(i).name   = toolTypes.get(moRng).name;
                items.get(i).amount = 1;
                items.get(i).tag    = 'T';
            }
        }
        if (rng == 453) { // homeward scroll
            items.get(i).name   = "homeward scroll";
            items.get(i).amount = 1;
            items.get(i).tag    = 'H';
        }
    }

    private void initialize() {
        for (int i = 0; i <= maxY; i++)
            for (int j = 0; j <= maxX; j++)
                map[i][j] = new Tile();
        for (int i = 0; i < messageArray.length; i++) messageArray[i] = "";
        initGuns();
        initAmmo();
        initTools();
        initEquip();
        initMonsters();
        initMisc();
        hero.face          = '@';
        hero.name          = "Eddy Pasterino";
        hero.life          = 100;
        hero.prevLife      = hero.life;
        hero.armor         = 1;
        hero.gold          = 0;
        hero.firearm       = gunTypes.get(1);
        hero.tool          = toolTypes.get(1);
        hero.ammo          = ammoTypes.get(1).quantity;
        hero.head          = equipTypes.get(0);
        hero.chest         = equipTypes.get(1);
        hero.glove         = equipTypes.get(0);
        hero.legs          = equipTypes.get(0);
        hero.ring1         = equipTypes.get(0);
        hero.ring2         = equipTypes.get(0);
        hero.state         = true;
        hero.blindTimer    = blindDuration;
        hero.paralyzeTimer = paralyzeDuration;
        hero.prevCell      = pass;
    }

    private void drawControls() {
        drawArrows();
        drawDefaultButtons();
    }

    private void drawArrows() {
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.home),  0,              27 * tileHeight,                   null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.up),    arrowWidth,     27 * tileHeight,                   null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pgup),  arrowWidth * 2, 27 * tileHeight,                   null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.left),  0,              27 * tileHeight + arrowHeight,     null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.wait),  arrowWidth,     27 * tileHeight + arrowHeight,     null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.right), arrowWidth * 2, 27 * tileHeight + arrowHeight,     null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.end),   0,              27 * tileHeight + arrowHeight * 2, null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.down),  arrowWidth,     27 * tileHeight + arrowHeight * 2, null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.pgdn),  arrowWidth * 2, 27 * tileHeight + arrowHeight * 2, null);
    }

    private void drawDefaultButtons() {
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.medpack),   screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.shoot),     screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.warp),      screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.inspect),   screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.inventory), screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.equip),     screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
    }

    private void drawConfirmationButtons() {
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight                    + tileHeight / 2,    null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth,                 27 * tileHeight                    + tileHeight / 2,    null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.confirm), screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight     + tileHeight * 1.5f, null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.button),  screenWidth - buttonWidth * 2 - tileWidth, 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
        canvas.drawBitmap(BitmapFactory.decodeResource(getResources(), R.drawable.cancel),  screenWidth - buttonWidth,                 27 * tileHeight + buttonHeight * 2 + tileHeight * 2.5f, null);
        imageView.invalidate();
    }

    private void townInstance() {
        emptyMap();
        clearItems();
        rooms.clear();
        rooms.add(new Room(0, 0, maxX / 3, maxY / 3, false));
        rooms.add(new Room(0, maxY - maxY / 3, maxX / 3, maxY, false));
        rooms.add(new Room(maxX - maxX / 3, 0, maxX, maxY / 3, false));
        rooms.add(new Room(maxX - maxX / 3, maxY - maxY / 3, maxX, maxY, false));
        addRoom(0, 0, maxX / 3, maxY / 3);
        addRoom(0, maxY - maxY / 3, maxX / 3, maxY);
        addRoom(maxX - maxX / 3, 0, maxX, maxY / 3);
        addRoom(maxX - maxX / 3, maxY - maxY / 3, maxX, maxY);
        int mY = maxY - 1, mX = 2;
        while (map[mY][mX].ch != wall) mY--; // bottom left room top wall
        map[mY][mX].ch       = pass;
        map[mY][maxX - 2].ch = pass;
        while (map[mY][mX].ch != wall) { // bottom -> top
            map[mY][mX - 1].ch   = wall;
            map[mY][mX].ch       = pass;
            map[mY][mX + 1].ch   = wall;
            map[mY][maxX - 1].ch = wall;
            map[mY][maxX - 2].ch = pass;
            map[mY][maxX - 3].ch = wall;
            mY--;
        }
        map[mY][mX].ch       = pass;
        map[mY][maxX - 2].ch = pass;
        mX = 1; mY = 2;
        while (map[mY][mX].ch != wall) mX++; // top left room right wall
        map[mY][mX].ch       = pass;
        map[maxY - 3][mX].ch = pass;
        while (map[mY][mX].ch != wall) { // left -> right
            map[mY - 1][mX].ch   = wall;
            map[mY][mX].ch       = pass;
            map[mY + 1][mX].ch   = wall;
            map[maxY - 1][mX].ch = wall;
            map[maxY - 2][mX].ch = pass;
            map[maxY - 3][mX].ch = wall;
            mX++;
        }
        map[mY][mX].ch       = pass;
        map[maxY - 2][mX].ch = pass;
        revealArea(0, 0, maxX, maxY);
        hero.x = 1;
        hero.y = maxY - 1;
        map[hero.y][hero.x].ch     = hero.face;
        map[hero.y][hero.x + 1].ch = stash;
        map[1][1].ch               = wall;
        map[1][3].ch               = wall;
        map[3][1].ch               = wall;
        map[3][3].ch               = wall;
        map[2][2].ch               = vendor;
        map[2][maxX - 2].ch        = waypoint;
        map[maxY - 1][maxX - 1].ch = stone;
        drawMap();
    }

    private void newInstance() {
        int   columnCount;
        Point exitCoords;

        emptyMap();
        columnCount = generateMap();
        connectRooms();
        clearItems();
        monsters.clear();
        hero.prevCell = pass;
        positionHero();
        exitCoords = positionExit(columnCount);
        if (progression < depth) progression = depth;
        if (depth % 10 == 0 && depth < maxDepth) {
            Item item = new Item();
            if (columnCount % 2 == 1) {
                item.x = exitCoords.x + 1;
                item.y = exitCoords.y + 1;
            } else {
                item.x = exitCoords.x + 1;
                item.y = exitCoords.y - 1;
            }
            item.name   = "flash bomb";
            item.amount = 1;
            item.tag    = 'F';
            items.add(item);
            map[items.get(items.size() - 1).y][items.get(items.size() - 1).x].ch = chest;
        }
        positionChests();
        positionMonsters();
        drawMap();
    }

    private void statusUpdate() {
        paint.setColor(Color.BLACK);
        canvas.drawRect(0, tileHeight * (maxY + 4) + 4, tileWidth * (maxX + 1), tileHeight * (maxY + 8) + 5, paint);
        paint.setColor(Color.LTGRAY);
        canvas.drawText("Life: " + Integer.toString(hero.life), 0, tileHeight * (maxY + 5), paint);
        hero.armor = hero.chest.armorValue + hero.head.armorValue + hero.legs.armorValue + hero.glove.armorValue + hero.ring1.armorValue + hero.ring2.armorValue;
        canvas.drawText("Armor: " + Integer.toString(hero.armor), 0, tileHeight * (maxY + 6), paint);
        paint.setColor(Color.GRAY);
        canvas.drawText(hero.firearm.name + " (" + Integer.toString(hero.ammo) + ")", 0, tileHeight * (maxY + 7), paint);
        canvas.drawText(hero.tool.name, 0, tileHeight * (maxY + 8), paint);
        paint.setColor(Color.LTGRAY);
        if (depth > 0) canvas.drawText("Depth: " + Integer.toString(depth), screenWidth - tileWidth * 9, tileHeight * (maxY + 5), paint);
        if (medpackCharge >  medpackHealAmount) paint.setColor(Color.parseColor("#2E64FE")); // bright blue
        if (medpackCharge == medpackHealAmount) paint.setColor(Color.parseColor("#0431B4")); // dark blue
        if (medpackCharge <  medpackHealAmount) paint.setColor(Color.DKGRAY);
        canvas.drawText("Life Flask", screenWidth - tileWidth * 9, tileHeight * (maxY + 6), paint);
        if (warpCounter >= warpDelay) paint.setColor(Color.GREEN); else paint.setColor(Color.DKGRAY);
        canvas.drawText("Warp", screenWidth - tileWidth * 9, tileHeight * (maxY + 7), paint);
        if (hero.blindTimer < blindDuration) {
            paint.setColor(Color.RED);
            canvas.drawText("Blind", screenWidth - tileWidth * (maxX / 2), tileHeight * (maxY + 5), paint);
        }
        if (hero.paralyzeTimer < paralyzeDuration) {
            paint.setColor(Color.RED);
            canvas.drawText("Paralyze", screenWidth - tileWidth * (maxX / 2), tileHeight * (maxY + 6), paint);
        }
        if (hero.life <= 0) {
            if (hero.ring1.name.equals("Ring of Sacrifice") || hero.ring2.name.equals("Ring of Sacrifice")) {
                hero.life = hero.prevLife;
                displayMessage("Revive! Ring of Sacrifice shatters.");
                if (hero.ring1.name.equals("Ring of Sacrifice")) hero.ring1.name = "";
                else if (hero.ring2.name.equals("Ring of Sacrifice")) hero.ring2.name = "";
            } else {
                new Thread() {
                    public void run() {
                        MainActivity.this.runOnUiThread(new Runnable() {
                            public void run() {
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        clearCell(hero.x, hero.y, Color.BLACK);
                                        paint.setColor(Color.RED);
                                        canvas.drawText(Character.toString(corpse), hero.x * tileWidth, (hero.y + 4) * tileHeight, paint);
                                        drawFrame(15, 8, 25, 10, "", "");
                                        paint.setColor(Color.WHITE);
                                        canvas.drawText(" R I P ", tileWidth * 17, tileHeight * 10, paint);
                                        imageView.invalidate();
                                    }
                                }, fxDelay * 5);
                                long now = System.currentTimeMillis();
                                long future = now + fxDelay * 5;
                                while (future > now) now = System.currentTimeMillis();
                            }
                        });
                        }
                }.start();
                File file = new File(MainActivity.this.getFilesDir().getAbsolutePath() + '/' + saveFileName);
                if (file.exists()) file.delete();
                gameState = '0';
            }
        }
        if (winCondition) {
            new Thread() {
                public void run() {
                    MainActivity.this.runOnUiThread(new Runnable() {
                        public void run() {
                            new Handler().postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    clearCell(hero.x, hero.y, Color.BLACK);
                                    paint.setColor(Color.WHITE);
                                    canvas.drawText(Character.toString(pass), hero.x * tileWidth, (hero.y + 4) * tileHeight, paint);
                                    drawFrame(11, 8, 29, 10, "", "");
                                    paint.setColor(Color.GREEN);
                                    canvas.drawText(" V I C T O R Y ", tileWidth * 13, tileHeight * 10, paint);
                                    imageView.invalidate();
                                }
                            }, fxDelay * 5);
                            long now    = System.currentTimeMillis();
                            long future = now + fxDelay * 5;
                            while (future > now) now = System.currentTimeMillis();
                        }
                    });
                }
            }.start();
            File file = new File(MainActivity.this.getFilesDir().getAbsolutePath() + '/' + saveFileName);
            if (file.exists()) file.delete();
            gameState = '9';
        }
    }

    private void flashCell(final int x, final int y, final int color, final int delay) {
        new Thread() {
            public void run() {
                MainActivity.this.runOnUiThread(new Runnable() {
                    public void run() {
                        paint.setColor(color);
                        canvas.drawText(Character.toString(map[y][x].ch), x * tileWidth, (y + 4) * tileHeight, paint);
                        imageView.invalidate();
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                drawMap();
                                imageView.invalidate();
                            }
                        }, delay);
                        long now    = System.currentTimeMillis();
                        long future = now + delay;
                        while (future > now) now = System.currentTimeMillis();
                    }
                });
            }
        }.start();
    }

    private void clearCell(int x, int y, int color) {
        paint.setColor(color);
        canvas.drawRect(x * tileWidth, (y + 3) * tileHeight + 4,
                x * tileWidth + tileWidth, (y + 3) * tileHeight + tileHeight + 4, paint);
    }

    private void clearFrame(int x1, int y1, int x2, int y2) {
        paint.setColor(Color.BLACK);
        canvas.drawRect(x1 * tileWidth, y1 * tileHeight + 3,
                x2 * tileWidth, (y2 + 1) * tileHeight + 8, paint);
    }

    private void clearMapHighlights() {
        for (int i = 0; i <= maxY; i++)
            for (int j = 0; j <= maxX; j++)
                map[i][j].highlighted = false;
    }

    private void clearMessages() { clearFrame(0, 0, maxX + 1, 2); }

    private void displayMessage(String message) {
        for (int i = 1; i < messageArray.length; i++) messageArray[i - 1] = messageArray[i];
        messageArray[messageArray.length - 1] = message;
        clearMessages();
        for (int i = 0; i < messageArray.length; i++) {
            paint.setColor(Color.LTGRAY);
            if (messageArray[i].contains("\"")) paint.setColor(Color.WHITE);
            canvas.drawText(messageArray[i], 0, i * tileHeight + tileHeight, paint);
        }
    }

    private void drawFrame(int x1, int y1, int x2, int y2, String header, String footer) {
        paint.setColor(Color.BLACK);
        canvas.drawRect(x1 * tileWidth, y1 * tileHeight + 4, x2 * tileWidth, (y2 + 1) * tileHeight + 4, paint);
        paint.setColor(Color.WHITE);
        canvas.drawRect(x1 * tileWidth + tileWidth / 2, y1 * tileHeight + tileHeight / 2 + 3,
                        x2 * tileWidth - tileWidth / 2, (y2 + 1) * tileHeight - tileHeight / 2 + 5, paint);
        paint.setColor(Color.BLACK);
        canvas.drawRect(x1 * tileWidth + tileWidth / 2 + 2, y1 * tileHeight + tileHeight / 2 + 5,
                x2 * tileWidth - tileWidth / 2 - 2, (y2 + 1) * tileHeight - tileHeight / 2 + 3, paint);
        if (header.length() > 0) {
            paint.setColor(Color.BLACK);
            canvas.drawRect(((x1 + (x2 - x1) / 2) - header.length() / 2) * tileWidth, y1 * tileHeight + 3,
                            ((x1 + (x2 - x1) / 2) - header.length() / 2 + header.length()) * tileWidth, (y1 + 1) * tileHeight + 4, paint);
            paint.setColor(Color.YELLOW);
            canvas.drawText(header, ((x1 + (x2 - x1) / 2) - header.length() / 2) * tileWidth + 4, (y1 + 1) * tileHeight, paint);
        }
        if (footer.length() > 0) {
            paint.setColor(Color.BLACK);
            canvas.drawRect(((x1 + (x2 - x1) / 2) - footer.length() / 2) * tileWidth, y2 * tileHeight + 4,
                            ((x1 + (x2 - x1) / 2) - footer.length() / 2 + footer.length()) * tileWidth, (y2 + 1) * tileHeight + 4, paint);
            paint.setColor(Color.YELLOW);
            canvas.drawText(footer, ((x1 + (x2 - x1) / 2) - footer.length() / 2) * tileWidth + 4, (y2 + 1) * tileHeight + 2, paint);
        }
    }

    private void positionMonsters() {
        int     x = 0, y = 0;
        Point[] theRoom = new Point[2];

        theRoom[0] = new Point(); theRoom[1] = new Point();
        for (int i = 0; i < rooms.size(); i++)
            if (hero.x > rooms.get(i).x1 && hero.x < rooms.get(i).x2 &&
                hero.y > rooms.get(i).y1 && hero.y < rooms.get(i).y2) {
                theRoom[0].x = rooms.get(i).x1 + 1; theRoom[0].y = rooms.get(i).y1 + 1;
                theRoom[1].x = rooms.get(i).x2 - 1; theRoom[1].y = rooms.get(i).y2 - 1;
                break;
            }
        for (int i = 0; i < rooms.size() + (depth / 5); i++) {
            while (map[y][x].ch != pass) {
                x = randomInt(1, maxX - 1);
                y = randomInt(1, maxY - 1);
            }
            int maxToughness = 0;
            if (depth <= 10)                maxToughness =  1;
            if (depth >  10 && depth <= 20) maxToughness =  2;
            if (depth >  20 && depth <= 30) maxToughness =  3;
            if (depth >  30 && depth <= 40) maxToughness =  4;
            if (depth >  40 && depth <= 50) maxToughness =  5;
            if (depth >  50 && depth <= 60) maxToughness =  6;
            if (depth >  60 && depth <= 70) maxToughness =  7;
            if (depth >  70 && depth <= 80) maxToughness =  8;
            if (depth >  80 && depth <= 90) maxToughness =  9;
            if (depth >  90)                maxToughness = 10;
            int k = randomInt(1, monsterTypes.size() - 1);
            while (monsterTypes.get(k).toughness > maxToughness)
                k = randomInt(1, monsterTypes.size() - 1);
            Player monster        = new Player();
            monster.x             = x;
            monster.y             = y;
            monster.face          = monsterTypes.get(k).face;
            monster.name          = monsterTypes.get(k).name;
            monster.life          = monsterTypes.get(k).life;
            monster.armor         = monsterTypes.get(k).armor;
            monster.firearm       = gunTypes.get(0);
            for (int j = 1; j < gunTypes.size(); j++)
                if (gunTypes.get(j).name.equals(monsterTypes.get(k).weapon)) {
                    monster.firearm = gunTypes.get(j);
                    break;
                }
            monster.tool          = toolTypes.get(0);
            for (int j = 1; j < toolTypes.size(); j++)
                if (toolTypes.get(j).name.equals(monsterTypes.get(k).weapon)) {
                    monster.tool = toolTypes.get(j);
                    break;
                }
            monster.pursuit       = monster.x >= theRoom[0].x && monster.x <= theRoom[1].x &&
                                    monster.y >= theRoom[0].y && monster.y <= theRoom[1].y;
            monster.blindTimer    = blindDuration;
            monster.paralyzeTimer = paralyzeDuration;
            monster.state         = true;
            monster.prevCell      = pass;
            map[y][x].ch          = monster.face;
            monsters.add(monster);
        }
        if (depth == maxDepth) { // final boss
            while (map[y][x].ch != pass) {
                x = randomInt(1, maxX - 1);
                y = randomInt(1, maxY - 1);
            }
            Player monster        = new Player();
            monster.x             = x;
            monster.y             = y;
            monster.face          = monsterTypes.get(monsterTypes.size() - 1).face;
            monster.name          = monsterTypes.get(monsterTypes.size() - 1).name;
            monster.life          = monsterTypes.get(monsterTypes.size() - 1).life;
            monster.armor         = monsterTypes.get(monsterTypes.size() - 1).armor;
            monster.firearm       = gunTypes.get(0);
            for (int i = 1; i < gunTypes.size(); i++)
                if (gunTypes.get(i).name.equals(monsterTypes.get(monsterTypes.size() - 1).weapon)) {
                    monster.firearm = gunTypes.get(i);
                    break;
                }
            monster.tool          = toolTypes.get(0);
            for (int i = 1; i < toolTypes.size(); i++)
                if (toolTypes.get(i).name.equals(monsterTypes.get(monsterTypes.size() - 1).weapon)) {
                    monster.tool = toolTypes.get(i);
                    break;
                }
            monster.pursuit       = false;
            monster.blindTimer    = blindDuration;
            monster.paralyzeTimer = paralyzeDuration;
            monster.state         = true;
            monster.prevCell      = pass;
            map[y][x].ch          = monster.face;
            monsters.add(monster);
        }
    }

    private void positionChests() {
        int i          = 0;
        int chestCount = 2;

        int rng = randomInt(0, 100);
        if (rng < 10) chestCount = 3;
        if (rng > 90) chestCount = 1;
        while (i < chestCount) {
            int x = randomInt(1, maxX - 1);
            int y = randomInt(1, maxY - 1);
            for (int j = 0; j < rooms.size(); j++) {
                if (x > rooms.get(j).x1 + 1 && x < rooms.get(j).x2 - 1 &&
                    y > rooms.get(j).y1 + 1 && y < rooms.get(j).y2 - 1 && map[y][x].ch == pass) {
                    map[y][x].ch = chest;
                    Item item    = new Item();
                    item.x       = x;
                    item.y       = y;
                    item.name    = "unknown";
                    items.add(item);
                    break;
                }
            }
            if (map[y][x].ch == chest) i++;
        }
    }

    private void positionHero() {
        int pX = 1, pY = maxY;

        while (map[pY][pX].ch != pass) pY--;
        hero.x = pX;
        hero.y = pY;
        map[pY][pX].ch = hero.face;
    }

    private Point positionExit(int columnCount) {
        int   pX = maxX, pY = 0;
        Point exitCoords = new Point();

        if (columnCount % 2 == 1) {
            while (map[pY][pX].ch != wall) pX--;
            pX -= 2;
        } else {
            while (map[pY][pX].ch != wall) pX--;
            while (map[pY][pX].ch == wall) pX--;
            pX++; // (pY, pX) = last set of rooms top left
            pY = maxY;
            while (map[pY][pX].ch != wall) pY--; // bottom left
            while (map[pY][pX].ch == wall && pX < maxX) pX++;
            if (map[pY][pX].ch != wall) pX -= 3; else pX -= 2;
        }
        map[pY][pX].ch = door;
        exitCoords.x = pX;
        exitCoords.y = pY;
        return exitCoords;
    }

    private void clearItems() {
        if (items.size() < 1) return;
        for (int i = 0; i < items.size(); i++)
            if (items.get(i).x != 0 && items.get(i).x != 100) items.remove(i);
    }

    private void emptyMap() {
        for (int i = 0; i <= maxY; i++)
            for (int j = 0; j <= maxX; j++) {
                map[i][j].ch          = space;
                map[i][j].revealed    = false;
                map[i][j].highlighted = false;
            }
    }

    private void drawMap() {
        Point[] theRoom = new Point[2];

        paint.setColor(Color.BLACK);
        canvas.drawRect(0, 3 * tileHeight + 4, (maxX + 1) * tileWidth, 3 * tileHeight + tileHeight * (maxY + 1) + 4, paint);
        for (int y = hero.y - 1; y <= hero.y + 1; y++)
            for (int x = hero.x - 1; x <= hero.x + 1; x++)
                map[y][x].revealed = true;
        theRoom[0] = new Point(); theRoom[1] = new Point();
        theRoom[0].x = -1; theRoom[0].y = -1;
        theRoom[1].x = -1; theRoom[1].y = -1;
        for (int i = 0; i < rooms.size(); i++)
            if (hero.x > rooms.get(i).x1 && hero.x < rooms.get(i).x2 &&
                hero.y > rooms.get(i).y1 && hero.y < rooms.get(i).y2) {
                theRoom[0].x = rooms.get(i).x1; theRoom[0].y = rooms.get(i).y1;
                theRoom[1].x = rooms.get(i).x2; theRoom[1].y = rooms.get(i).y2;
                revealArea(rooms.get(i).x1, rooms.get(i).y1, rooms.get(i).x2, rooms.get(i).y2);
                rooms.get(i).revealed = true;
                break;
            }
        for (int i = 0; i <= maxY; i++)
            for (int j = 0; j <= maxX; j++) {
                if (map[i][j].highlighted) {
                    paint.setColor(Color.BLUE);
                    canvas.drawRect(j * tileWidth, (i + 3) * tileHeight + 4,
                                    j * tileWidth + tileWidth, (i + 3) * tileHeight + tileHeight + 4, paint);
                }
                if (i >= theRoom[0].y && i <= theRoom[1].y &&
                    j >= theRoom[0].x && j <= theRoom[1].x)
                     paint.setColor(Color.WHITE);
                else paint.setColor(Color.GRAY);
                switch (map[i][j].ch) {
                    case stash    :
                    case chest    : paint.setColor(Color.YELLOW); break;
                    case clip     : paint.setColor(Color.CYAN); break;
                    case corpse   : paint.setColor(Color.RED); break;
                    case vendor   : paint.setColor(Color.GREEN); break;
                    case waypoint : paint.setColor(Color.CYAN); break;
                    case stone    : paint.setColor(Color.MAGENTA); break;
                }
                if (map[i][j].ch == monsterTypes.get(monsterTypes.size() - 1).face) paint.setColor(Color.MAGENTA);
                if (map[i][j].revealed)
                    if (hero.blindTimer < blindDuration) {
                        switch (map[i][j].ch) {
                            case wall   :
                            case pass   :
                            case corpse :
                            case space  :
                            case door   : canvas.drawText(Character.toString(map[i][j].ch), j * tileWidth, (i + 4) * tileHeight, paint); break;
                            default     : if (i >= theRoom[0].y && i <= theRoom[1].y && j >= theRoom[0].x && j <= theRoom[1].x)
                                               paint.setColor(Color.WHITE);
                                          else paint.setColor(Color.GRAY);
                                          canvas.drawText(Character.toString(pass), j * tileWidth, (i + 4) * tileHeight, paint);
                        }
                        if (map[i][j].ch == hero.face) canvas.drawText(Character.toString(map[i][j].ch), j * tileWidth, (i + 4) * tileHeight, paint);
                    } else {
                        switch (map[i][j].ch) {
                            case stash :
                            case chest : canvas.drawText("_", j * tileWidth, (i + 4) * tileHeight,     paint);
                                         canvas.drawText("_", j * tileWidth, (i + 4) * tileHeight - 2, paint);
                                         canvas.drawText("_", j * tileWidth, (i + 4) * tileHeight - 4, paint);
                                         canvas.drawText("_", j * tileWidth, (i + 4) * tileHeight - 6, paint);
                                         break;
                            default    : canvas.drawText(Character.toString(map[i][j].ch), j * tileWidth, (i + 4) * tileHeight, paint);
                        }
                    }
            }
    }

    private void revealArea(int x1, int y1, int x2, int y2) {
        for (int i = y1; i <= y2; i++)
            for (int j = x1; j <= x2; j++)
                map[i][j].revealed = true;
    }

    private int generateMap() {
        int    x1, y1, x2, y2;
        int    rX          = 0;
        int    rY          = 0;
        int    tempX       = 0;
        int    columnCount = 0;

        rooms.clear();
        while (true) {
            x1 = rX;
            y1 = rY;
            x2 = rX + randomInt(4, maxX / 2);
            y2 = rY + randomInt(4, maxY);
            if (x2 > maxX) x2 = maxX;
            if (y2 > maxY) y2 = maxY;
            if (x2 > tempX) tempX = x2;
            if (y2 <= maxY - 6) rY = y2 + 2; else { rY = 0; columnCount++; }
            if ((x2 <= maxX - 6) && rY == 0) rX = tempX + 2;
            rooms.add(new Room(x1, y1, x2, y2, false));
            addRoom(x1, y1, x2, y2);
            if ((tempX > maxX - 6) && (y2 > maxY - 6)) break;
        }
        return columnCount;
    }

    private void addRoom(int x1, int y1, int x2, int y2) {
        for (int i = x1; i <= x2; i++)
            for (int j = y1; j <= y2; j++)
                if (((i == x1) || (i == x2) || (j == y1) || (j == y2))) map[j][i].ch = wall;
                else map[j][i].ch = pass;
    }

    private void connectRooms() {
        int pX, pY, cX, tX;

        pX = 2; pY = maxY;
        while (true) { // main loop
            while (pY > 5) { // bottom -> top sequence
                while (map[pY][pX].ch != wall) pY--;
                while (pY > 0 && map[pY - 1][pX].ch == pass) pY--;
                if (map[pY][pX].ch == pass) pY--; // (pY, pX - 2) = top left corner
                if (pY < 6) break;
                cX = pX;
                while (map[pY][cX].ch == wall && cX < maxX) cX++;
                if (map[pY][cX].ch != wall) cX -= 3; else cX -= 2; // (pY, cX) = top right passable
                while (!(map[pY + 1][cX + 1].ch == pass &&
                         map[pY + 1][cX].ch     == pass &&
                         map[pY + 1][cX - 1].ch == pass &&
                         map[pY - 3][cX + 1].ch == pass &&
                         map[pY - 3][cX].ch     == pass &&
                         map[pY - 3][cX - 1].ch == pass)) cX--; // looking for connectable spot
                map[pY][cX].ch         = pass;
                map[pY - 1][cX].ch     = pass;
                map[pY - 2][cX].ch     = pass;
                map[pY - 1][cX - 1].ch = wall;
                map[pY - 1][cX + 1].ch = wall;
                pY -= 2;
                while (map[pY - 1][pX].ch == pass && pY > 0) pY--;
                if (map[pY][pX].ch == pass) pY--; // (pY, pX) = top left passable
                if (pY < 6) break;
                map[pY][pX].ch         = pass;
                map[pY - 1][pX].ch     = pass;
                map[pY - 2][pX].ch     = pass;
                map[pY - 1][pX - 1].ch = wall;
                map[pY - 1][pX + 1].ch = wall;
                pY -= 2;
            }
            pY = 0; // left -> right sequence
            while (map[pY][pX].ch == wall && pX < maxX) pX++;
            if (map[pY][pX].ch != wall) pX--; // rightmost wall
            if (pX > maxX - 6) break;
            cX = pX + 1;
            while (map[pY][cX].ch != wall && cX < maxX) cX++; // (pY, cX) = next set of rooms top left
            if (map[pY][cX].ch != wall) break;
            map[pY + 2][pX].ch = pass;
            for (int i = pX + 1; i <= cX - 1; i++) {
                map[pY + 1][i].ch = wall;
                map[pY + 2][i].ch = pass;
                map[pY + 3][i].ch = wall;
            }
            map[pY + 2][cX].ch = pass;
            pX = cX + 2; pY = 0; cX = pX; // top -> bottom sequence
            while (pY < maxY - 5) {
                while (map[pY][cX].ch == wall && cX < maxX) cX++;
                if (map[pY][cX].ch != wall) cX--; // right wall
                while (map[pY][cX].ch == wall && pY < maxY) pY++;
                if (map[pY][cX].ch != wall) pY--; // (pY, cX) = bottom right corner
                if (pY > maxY - 6) break;
                cX -= 2; // possible passable (rightmost)
                while (!(map[pY - 1][cX + 1].ch == pass &&
                         map[pY - 1][cX].ch     == pass &&
                         map[pY - 1][cX - 1].ch == pass &&
                         map[pY + 3][cX + 1].ch == pass &&
                         map[pY + 3][cX].ch     == pass &&
                         map[pY + 3][cX - 1].ch == pass)) cX--; // (pY + 1, cX) = connectable spot
                map[pY][cX].ch         = pass;
                map[pY + 1][cX].ch     = pass;
                map[pY + 2][cX].ch     = pass;
                map[pY + 1][cX - 1].ch = wall;
                map[pY + 1][cX + 1].ch = wall;
                pY += 2;
                while (map[pY][pX - 2].ch == wall && pY < maxY) pY++;
                if (map[pY][pX - 2].ch != wall) pY--; // bottom wall
                if (pY > maxY - 6) break;
                map[pY][pX].ch         = pass;
                map[pY + 1][pX].ch     = pass;
                map[pY + 2][pX].ch     = pass;
                map[pY + 1][pX - 1].ch = wall;
                map[pY + 1][pX + 1].ch = wall;
                pY += 2; cX = pX + 1;
            }
            pY = 0; tX = pX - 2; // right -> left sequence
            while (map[pY][pX].ch == wall && pX < maxX) pX++;
            if (pX > maxX - 5) break;
            while (map[pY][pX].ch != wall && pX < maxX) pX++;
            if (pX > maxX - 4) break;
            if (map[pY][pX].ch != wall) pX--; pY = maxY; // next set
            while (map[pY][pX].ch != wall) pY--; // (pY, pX) = bottom left corner
            pY -= 2;     // (pY, pX) = passable spot
            cX = pX - 2; // (pY, cX) = ~possible~ previous set passable spot
            while (true) {
                while (map[pY][cX].ch != wall && cX > tX) cX--; // previous set right wall
                if (cX == tX) {
                    pY--; cX = pX - 2;
                    continue;
                }
                if (map[pY - 1][pX + 1].ch == pass &&
                    map[pY][pX + 1].ch     == pass &&
                    map[pY + 1][pX + 1].ch == pass &&
                    map[pY - 1][cX - 1].ch == pass &&
                    map[pY][cX - 1].ch     == pass &&
                    map[pY + 1][cX - 1].ch == pass &&
                    map[pY - 1][cX + 1].ch == space &&
                    map[pY][cX + 1].ch     == space &&
                    map[pY + 1][cX + 1].ch == space) {
                        map[pY][cX].ch = pass;
                        for (int i = cX + 1; i <= pX - 1; i++) {
                            map[pY - 1][i].ch = wall;
                            map[pY][i].ch     = pass;
                            map[pY + 1][i].ch = wall;
                        }
                        map[pY][pX].ch = pass;
                    break;
                }
                pY--; cX = pX - 2;
            }
            pX += 2; pY = maxY;
        }
    }

    private String capitalize(String string) {
        return string.length() == 0 ? string : string.substring(0, 1).toUpperCase() + string.substring(1);
    }

    private int randomInt(int min, int max) {
        return random.nextInt((max - min) + 1) + min;
    }

    private void initGuns() {
        gunTypes.add(new Weapon("empty",                      0, 0,  0));
        gunTypes.add(new Weapon("short bow",                 10, 2,  1));
        gunTypes.add(new Weapon("reflex bow",                12, 3,  1));
        gunTypes.add(new Weapon("composite bow",             13, 4,  3));
        gunTypes.add(new Weapon("black bow",                 17, 4,  4));
        gunTypes.add(new Weapon("light crossbow",            12, 5,  2));
        gunTypes.add(new Weapon("heavy crossbow",            15, 5,  4));
        gunTypes.add(new Weapon("sniper crossbow",           20, 1,  5));
        gunTypes.add(new Weapon("pistol",                    11, 3,  1));
        gunTypes.add(new Weapon("revolver",                  13, 7,  2));
        gunTypes.add(new Weapon("rifle",                     14, 4,  4));
        gunTypes.add(new Weapon("musket",                    16, 4,  5));
        gunTypes.add(new Weapon("double rifle",              22, 2,  5));
        gunTypes.add(new Weapon("elephant gun",              27, 3,  6));
        gunTypes.add(new Weapon("shotgun",                   30, 7,  7));
        gunTypes.add(new Weapon("Long Bow of the Dark",      28, 7, 10)); // 10% chance to blind enemy
        gunTypes.add(new Weapon("Hand Cannon",               50, 0, 10));
    }

    private void initAmmo() {
        ammoTypes.add(new Ammo("empty",              "empty",             0,  0));
        ammoTypes.add(new Ammo("arrows",             " bow",             10,  5));
        ammoTypes.add(new Ammo("crossbow bolts",     "crossbow",          8,  5));
        ammoTypes.add(new Ammo("pistol bullets",     "pistol",           12,  5));
        ammoTypes.add(new Ammo("revolver bullets",   "revolver",         10,  5));
        ammoTypes.add(new Ammo("lead balls",         "musket",           10,  6));
        ammoTypes.add(new Ammo("rifle rounds",       "rifle",            10,  7));
        ammoTypes.add(new Ammo("big bullets",        "elephant gun",      8, 10));
        ammoTypes.add(new Ammo("shotgun shells",     "shotgun",           8, 10));
        ammoTypes.add(new Ammo("cannon balls",       "Hand Cannon",       4, 20));
    }

    private void initTools() {
        toolTypes.add(new Weapon("empty",                    0, 0,  0));
        toolTypes.add(new Weapon("fists",                    5, 1,  0));
        toolTypes.add(new Weapon("bandit knife",             7, 1,  1));
        toolTypes.add(new Weapon("iron claw",                7, 2,  1));
        toolTypes.add(new Weapon("beast claw",               8, 2,  1));
        toolTypes.add(new Weapon("dagger",                   9, 1,  1));
        toolTypes.add(new Weapon("hunter axe",               9, 2,  1));
        toolTypes.add(new Weapon("reinforced club",         10, 3,  2));
        toolTypes.add(new Weapon("short sword",             10, 1,  2));
        toolTypes.add(new Weapon("morning star",            10, 2,  3));
        toolTypes.add(new Weapon("large club",              11, 3,  3));
        toolTypes.add(new Weapon("notched whip",            12, 2,  3));
        toolTypes.add(new Weapon("long sword",              15, 1,  3));
        toolTypes.add(new Weapon("blacksmith hammer",       15, 2,  4));
        toolTypes.add(new Weapon("bastard sword",           17, 1,  4));
        toolTypes.add(new Weapon("moonlight sword",         18, 1,  4));
        toolTypes.add(new Weapon("silver sword",            18, 3,  5));
        toolTypes.add(new Weapon("battle axe",              18, 2,  5));
        toolTypes.add(new Weapon("great mace",              18, 4,  5));
        toolTypes.add(new Weapon("ghost blade",             19, 2,  5));
        toolTypes.add(new Weapon("crystal sword",           20, 2,  6));
        toolTypes.add(new Weapon("dark sword",              20, 3,  6));
        toolTypes.add(new Weapon("claymore",                21, 2,  6));
        toolTypes.add(new Weapon("holy blade",              23, 3,  7));
        toolTypes.add(new Weapon("obsidian sword",          24, 3,  7));
        toolTypes.add(new Weapon("chaos blade",             25, 5,  7));
        toolTypes.add(new Weapon("Sword of the Abyss",      32, 2, 10)); // 15% chance to warp enemy away
        toolTypes.add(new Weapon("Hammer of the Inferno",   30, 5, 10)); // 25% chance of additional 8-12 damage
        toolTypes.add(new Weapon("Scythe of the Gravelord", 35, 5, 10)); // 20% chance to reduce enemy's life by 30%
    }

    private void initEquip() {
        equipTypes.add(new Equip("empty",                 "empty",   0,  0));

        equipTypes.add(new Equip("sweaty clothes",        "chest",   1,  1));
        equipTypes.add(new Equip("sorcerer cloak",        "chest",   4,  1));
        equipTypes.add(new Equip("cloth robe",            "chest",   5,  1));
        equipTypes.add(new Equip("noble dress",           "chest",   6,  1));
        equipTypes.add(new Equip("wanderer coat",         "chest",   7,  1));
        equipTypes.add(new Equip("graveguard robe",       "chest",   8,  1));
        equipTypes.add(new Equip("witch cloak",           "chest",   9,  1));
        equipTypes.add(new Equip("cleric robe",           "chest",  10,  2));
        equipTypes.add(new Equip("crimson robe",          "chest",  11,  2));
        equipTypes.add(new Equip("holy robe",             "chest",  12,  2));
        equipTypes.add(new Equip("maiden dress",          "chest",  13,  3));
        equipTypes.add(new Equip("mage coat",             "chest",  14,  3));
        equipTypes.add(new Equip("thief robe",            "chest",  15,  3));
        equipTypes.add(new Equip("hunter cloak",          "chest",  16,  3));
        equipTypes.add(new Equip("moonlight robe",        "chest",  17,  4));
        equipTypes.add(new Equip("long coat",             "chest",  18,  4));
        equipTypes.add(new Equip("black cloak",           "chest",  19,  4));
        equipTypes.add(new Equip("shadow garb",           "chest",  20,  4));
        equipTypes.add(new Equip("leather armor",         "chest",  21,  4));
        equipTypes.add(new Equip("chain armor",           "chest",  22,  5));
        equipTypes.add(new Equip("iron armor",            "chest",  23,  5));
        equipTypes.add(new Equip("steel armor",           "chest",  24,  5));
        equipTypes.add(new Equip("knight armor",          "chest",  25,  6));
        equipTypes.add(new Equip("silver armor",          "chest",  26,  6));
        equipTypes.add(new Equip("paladin armor",         "chest",  30,  6));
        equipTypes.add(new Equip("crystal armor",         "chest",  35,  7));
        equipTypes.add(new Equip("dark armor",            "chest",  45,  7));
        equipTypes.add(new Equip("bone armor",            "chest",  50,  7));
        equipTypes.add(new Equip("Armor of the Glorious", "chest", 120, 10)); // 10% chance to paralyze enemy on attack
        equipTypes.add(new Equip("Armor of the Sun",      "chest", 200, 10)); // 20% chance of getting blind on attack
        equipTypes.add(new Equip("Armor of Thorns",       "chest",  75, 10)); // enemy takes 10-12 damage on attack

        equipTypes.add(new Equip("cloth hood",              "head",  3,  1));
        equipTypes.add(new Equip("wanderer hood",           "head",  4,  1));
        equipTypes.add(new Equip("witch hat",               "head",  5,  1));
        equipTypes.add(new Equip("hunter hat",              "head",  6,  1));
        equipTypes.add(new Equip("maiden hood",             "head",  7,  1));
        equipTypes.add(new Equip("mage hat",                "head",  8,  1));
        equipTypes.add(new Equip("sorcerer hat",            "head",  9,  1));
        equipTypes.add(new Equip("black hood",              "head", 10,  2));
        equipTypes.add(new Equip("priest hat",              "head", 11,  2));
        equipTypes.add(new Equip("thief hood",              "head", 12,  2));
        equipTypes.add(new Equip("madman hood",             "head", 13,  2));
        equipTypes.add(new Equip("chain helm",              "head", 14,  3));
        equipTypes.add(new Equip("iron helm",               "head", 15,  3));
        equipTypes.add(new Equip("steel helm",              "head", 16,  3));
        equipTypes.add(new Equip("cleric helm",             "head", 17,  4));
        equipTypes.add(new Equip("knight helm",             "head", 18,  4));
        equipTypes.add(new Equip("guardian helm",           "head", 19,  4));
        equipTypes.add(new Equip("crystal helm",            "head", 20,  4));
        equipTypes.add(new Equip("moonlight crown",         "head", 21,  4));
        equipTypes.add(new Equip("royal helm",              "head", 22,  5));
        equipTypes.add(new Equip("paladin helm",            "head", 23,  5));
        equipTypes.add(new Equip("silver mask",             "head", 24,  5));
        equipTypes.add(new Equip("dark mask",               "head", 25,  6));
        equipTypes.add(new Equip("shadow mask",             "head", 26,  6));
        equipTypes.add(new Equip("bone mask",               "head", 27,  7));
        equipTypes.add(new Equip("Crown of Dusk",           "head", 30, 10)); // 10% chance to blind enemy on attack
        equipTypes.add(new Equip("Crown of the Sun",        "head", 50, 10)); // 20% chance to miss an attack
        equipTypes.add(new Equip("Crown of the Great Lord", "head", 40, 10)); // 10% chance to double damage of attack
        equipTypes.add(new Equip("Helm of Thorns",          "head", 35, 10)); // enemy takes 3-4 damage on attack

        equipTypes.add(new Equip("wanderer gloves",             "glove",  3,  1));
        equipTypes.add(new Equip("surgical gloves",             "glove",  4,  1));
        equipTypes.add(new Equip("leather gloves",              "glove",  5,  1));
        equipTypes.add(new Equip("hunter gloves",               "glove",  6,  1));
        equipTypes.add(new Equip("thief gloves",                "glove",  7,  1));
        equipTypes.add(new Equip("cleric gauntlets",            "glove",  8,  1));
        equipTypes.add(new Equip("antiquated gloves",           "glove",  9,  1));
        equipTypes.add(new Equip("sorcerer gauntlets",          "glove", 10,  2));
        equipTypes.add(new Equip("mage gauntlets",              "glove", 11,  2));
        equipTypes.add(new Equip("maiden gloves",               "glove", 12,  2));
        equipTypes.add(new Equip("silver gauntlets",            "glove", 14,  3));
        equipTypes.add(new Equip("moonlight gloves",            "glove", 15,  3));
        equipTypes.add(new Equip("paladin gauntlets",           "glove", 16,  3));
        equipTypes.add(new Equip("iron gauntlets",              "glove", 17,  4));
        equipTypes.add(new Equip("steel gauntlets",             "glove", 18,  4));
        equipTypes.add(new Equip("knight gauntlets",            "glove", 19,  4));
        equipTypes.add(new Equip("crimson gloves",              "glove", 20,  4));
        equipTypes.add(new Equip("crystal gauntlets",           "glove", 21,  4));
        equipTypes.add(new Equip("guardian gauntlets",          "glove", 22,  5));
        equipTypes.add(new Equip("bone encrusted gloves",       "glove", 23,  5));
        equipTypes.add(new Equip("dark gauntlets",              "glove", 25,  6));
        equipTypes.add(new Equip("shadow gauntlets",            "glove", 27,  7));
        equipTypes.add(new Equip("Bracelet of the Great Lord",  "glove", 30, 10)); // 20% of melee damage leeched as life
        equipTypes.add(new Equip("Gauntlets of the Vanquisher", "glove", 40, 10)); // add 8-10 damage to bare-handed attacks
        equipTypes.add(new Equip("Gauntlets of Thorns",         "glove", 35, 10)); // enemy takes 5-8 damage on attack

        equipTypes.add(new Equip("sorcerer boots",        "legs",  4,  1));
        equipTypes.add(new Equip("mage boots",            "legs",  5,  1));
        equipTypes.add(new Equip("wanderer boots",        "legs",  6,  1));
        equipTypes.add(new Equip("maiden skirt",          "legs",  7,  1));
        equipTypes.add(new Equip("witch waistcloth",      "legs",  8,  1));
        equipTypes.add(new Equip("black tights",          "legs",  9,  1));
        equipTypes.add(new Equip("leather boots",         "legs", 10,  2));
        equipTypes.add(new Equip("hunter trousers",       "legs", 11,  2));
        equipTypes.add(new Equip("cleric leggings",       "legs", 12,  2));
        equipTypes.add(new Equip("chain leggings",        "legs", 13,  2));
        equipTypes.add(new Equip("crimson waistcloth",    "legs", 14,  3));
        equipTypes.add(new Equip("guardian leggings",     "legs", 15,  3));
        equipTypes.add(new Equip("thief tights",          "legs", 16,  3));
        equipTypes.add(new Equip("moonlight waistcloth",  "legs", 17,  4));
        equipTypes.add(new Equip("iron leggings",         "legs", 18,  4));
        equipTypes.add(new Equip("steel leggings",        "legs", 19,  4));
        equipTypes.add(new Equip("knight leggings",       "legs", 20,  4));
        equipTypes.add(new Equip("silver leggings",       "legs", 21,  4));
        equipTypes.add(new Equip("paladin leggings",      "legs", 22,  5));
        equipTypes.add(new Equip("crystal leggings",      "legs", 23,  5));
        equipTypes.add(new Equip("dark leggings",         "legs", 24,  5));
        equipTypes.add(new Equip("shadow leggings",       "legs", 25,  6));
        equipTypes.add(new Equip("bone leggings",         "legs", 27,  7));
        equipTypes.add(new Equip("Boots of the Explorer", "legs", 35, 10)); // double warp recharge speed
        equipTypes.add(new Equip("Boots of Evasion",      "legs", 30, 10)); // 20% chance to evade attack
        equipTypes.add(new Equip("Leggings of Thorns",    "legs", 32, 10)); // enemy takes 2-3 damage on attack

        equipTypes.add(new Equip("Dragon Ring",       "ring",  0, 10)); // boost damage 20%
        equipTypes.add(new Equip("Blood Ring",        "ring",  0, 10)); // 10% chance to instantly kill enemy, wearer loses 10 life each turn
        equipTypes.add(new Equip("Hawk Ring",         "ring",  0, 10)); // +25% ranged weapon damage
        equipTypes.add(new Equip("Wolf Ring",         "ring",  0, 10)); // +25% melee weapon damage
        equipTypes.add(new Equip("Calamity Ring",     "ring",  0, 10)); // doubles damage taken
        equipTypes.add(new Equip("Covetous Ring",     "ring",  0, 10)); // increase item discovery 50%
        equipTypes.add(new Equip("Gold Ring",         "ring",  0, 10)); // increase gold acquisition 50%
        equipTypes.add(new Equip("Stone Ring",        "ring", 30, 10)); // +30 armor
        equipTypes.add(new Equip("Cling Ring",        "ring",  0, 10)); // gain 30 life from fallen enemies
        equipTypes.add(new Equip("Ring of Sacrifice", "ring",  0, 10)); // prevent lethal damage, but ring breaks
        equipTypes.add(new Equip("Ancient Ring",      "ring",  0, 10)); // flask restores 50 additional life per use
        equipTypes.add(new Equip("Quartz Ring",       "ring",  0, 10)); // add 5-10 damage to ranged attacks
        equipTypes.add(new Equip("Warrior Ring",      "ring",  0, 10)); // add 5-10 damage to melee attacks
        equipTypes.add(new Equip("Redeye Ring",       "ring",  0, 10)); // wearer is immune to blind
        equipTypes.add(new Equip("Silver Ring",       "ring",  0, 10)); // wearer is immune to paralyze
        equipTypes.add(new Equip("Vanquisher Ring",   "ring",  0, 10)); // 500% increased bare-handed attack power
    }

    private void initMonsters() {
        monsterTypes.add(new Monster(' ', "empty",            0, 0,                 "empty",               0));

        monsterTypes.add(new Monster('r', "giant rat",       10, 0,                 "bandit knife",        1));
        monsterTypes.add(new Monster('m', "molerat",         10, arMitigation,      "dagger",              1));
        monsterTypes.add(new Monster('b', "giant bat",        8, 0,                 "hunter axe",          1));
        monsterTypes.add(new Monster('i', "wisp",            10, 0,                 "musket",              1));
        monsterTypes.add(new Monster('c', "scavenger",       13, 0,                 "pistol",              1));
        monsterTypes.add(new Monster('w', "warg",            14, 0,                 "beast claw",          1));

        monsterTypes.add(new Monster('h', "ghost",           14, arMitigation,      "revolver",            2));
        monsterTypes.add(new Monster('z', "zombie",          15, 0,                 "reinforced club",     2));
        monsterTypes.add(new Monster('g', "ghoul",           15, arMitigation,      "short sword",         2));
        monsterTypes.add(new Monster('s', "skeleton",        14, arMitigation,      "morning star",        2));

        monsterTypes.add(new Monster('a', "revenant",        17, arMitigation *  2, "notched whip",        3));
        monsterTypes.add(new Monster('f', "shadowfolk",      15, arMitigation *  2, "large club",          3));
        monsterTypes.add(new Monster('n', "minecrawler",     17, arMitigation,      "rifle",               3));
        monsterTypes.add(new Monster('k', "skeleton mage",   18, arMitigation *  2, "heavy crossbow",      3)); // 30% chance to paralyze

        monsterTypes.add(new Monster('o', "gargoyle",        20, arMitigation *  2, "notched whip",        4));
        monsterTypes.add(new Monster('e', "werewolf",        23, arMitigation,      "long sword",          4));
        monsterTypes.add(new Monster('v', "vampire",         25, arMitigation *  2, "bastard sword",       4)); // 30% of melee damage leeched back as life
        monsterTypes.add(new Monster('p', "shapeshifter",    21, arMitigation,      "double rifle",        4));

        monsterTypes.add(new Monster('C', "chimera",         27, arMitigation *  2, "blacksmith hammer",   5));
        monsterTypes.add(new Monster('A', "salamander",      26, arMitigation,      "moonlight sword",     5));
        monsterTypes.add(new Monster('W', "witch",           25, arMitigation,      "sniper crossbow",     5)); // 30% chance to blind

        monsterTypes.add(new Monster('S', "succubus",        30, arMitigation *  2, "silver sword",        6)); // player has 30% chance to miss hit
        monsterTypes.add(new Monster('I', "incubus",         32, arMitigation,      "battle axe",          6));
        monsterTypes.add(new Monster('U', "undead",          32, arMitigation,      "great mace",          6));

        monsterTypes.add(new Monster('T', "stone golem",     35, arMitigation *  2, "great mace",          7));
        monsterTypes.add(new Monster('N', "iron golem",      37, arMitigation *  2, "battle axe",          7));
        monsterTypes.add(new Monster('M', "demon",           40, arMitigation *  2, "ghost blade",         7));

        monsterTypes.add(new Monster('B', "shadow beast",    42, arMitigation *  2, "crystal sword",       7));
        monsterTypes.add(new Monster('O', "shadow warrior",  43, arMitigation *  2, "dark sword",          8));
        monsterTypes.add(new Monster('H', "shadow lord",     45, arMitigation *  2, "elephant gun",        8));

        monsterTypes.add(new Monster('D', "swamp dragon",    50, arMitigation *  2, "dark sword",          8)); // 20% chance of additional 16-18 damage
        monsterTypes.add(new Monster('G', "stone dragon",    50, arMitigation *  3, "claymore",            8)); // 20% chance to paralyze
        monsterTypes.add(new Monster('E', "bone dragon",     55, arMitigation *  2, "holy blade",          9)); // 20% chance to blind

        monsterTypes.add(new Monster('K', "seeker",          55, arMitigation,      "shotgun",             9));
        monsterTypes.add(new Monster('R', "lurker",          53, arMitigation *  2, "obsidian sword",      9));
        monsterTypes.add(new Monster('L', "demon lord",      60, arMitigation *  2, "chaos blade",         9)); // 50% chance to warp next to the player

        monsterTypes.add(new Monster('X', "Fest",           255, arMitigation * 10, "Hand Cannon",       255)); // 20% chance to reflect damage
    }

    private void initMisc() {
        miscStuff.add(new Misc("empty",       0));
        miscStuff.add(new Misc("empty",       0));
        miscStuff.add(new Misc("empty",       0));
        miscStuff.add(new Misc("empty",       0));
        miscStuff.add(new Misc("empty",       0));
        miscStuff.add(new Misc("empty",       0));
        miscStuff.add(new Misc("blood vial", 10));
        miscStuff.add(new Misc("blood vial", 10));
        miscStuff.add(new Misc("gold coin",   5));
        miscStuff.add(new Misc("gold coin",   5));
    }

    @Override
    public void onBackPressed() {
        if (hero.life > 0 && !winCondition)
            try {
                ObjectOutputStream stream = new ObjectOutputStream(new FileOutputStream(MainActivity.this.getFilesDir().getAbsolutePath() + '/' +saveFileName));
                stream.writeInt(depth);
                stream.writeInt(progression);
                stream.writeInt(medpackCharge);
                stream.writeInt(warpCounter);
                stream.writeObject(hero);
                stream.writeObject(map);
                stream.writeInt(rooms.size());
                for (Room r : rooms) stream.writeObject(r);
                stream.writeInt(items.size());
                if (items.size() > 0) for (Item i : items) stream.writeObject(i);
                stream.writeInt(monsters.size());
                if (monsters.size() > 0) for (Player m : monsters) stream.writeObject(m);
                stream.close();
            } catch (Exception e) {
                e.printStackTrace();
            }
        super.onBackPressed();
    }

    private void readState() {
        try {
            ObjectInputStream stream = new ObjectInputStream(new FileInputStream(MainActivity.this.getFilesDir().getAbsolutePath() + '/' +saveFileName));
            depth         = stream.readInt();
            progression   = stream.readInt();
            medpackCharge = stream.readInt();
            warpCounter   = stream.readInt();
            hero          = (Player)   stream.readObject();
            map           = (Tile[][]) stream.readObject();
            int roomCount = stream.readInt();
            for (int i = 0; i < roomCount; i++) rooms.add((Room) stream.readObject());
            int itemCount = stream.readInt();
            if (itemCount > 0)
                for (int i = 0; i < itemCount; i++) items.add((Item) stream.readObject());
            int monsterCount = stream.readInt();
            if (monsterCount > 0)
                for (int i = 0; i < monsterCount; i++) monsters.add((Player) stream.readObject());
            stream.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        // getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
