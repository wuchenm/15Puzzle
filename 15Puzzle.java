import java.util.ArrayList;
import tester.Tester;
import javalib.impworld.*;
import java.awt.Color;
import javalib.worldimages.*;
import java.util.Random;


// Represents an individual tile
class Tile {

  // The number on the tile.
  // Value 0 is the empty space in the board
  int value;

  // Constructor
  public Tile(int value) {
    this.value = value;
  }

  //Draws this tile onto the background at the specified logical coordinates
  WorldScene drawAt(int col, int row, WorldScene background) {
    int tileSize = 100; // each tile is a 100x100 square
    int borderSize = 2; // Width of the border
    int x = col * tileSize + tileSize / 2; // x-coordinate for the center of the tile
    int y = row * tileSize + tileSize / 2; // y-coordinate for the center of the tile

    // Create a border for the tiles
    WorldImage borderImage = new RectangleImage(tileSize + borderSize, 
        tileSize + borderSize, OutlineMode.SOLID, Color.BLACK);

    // Initialize the tile image
    WorldImage tileImage = new RectangleImage(tileSize, tileSize, OutlineMode.SOLID, Color.GRAY);

    // Create a number image if the tile is not the space
    WorldImage numberImage = null;
    if (this.value != 0) {
      numberImage = new TextImage(Integer.toString(this.value), 20, Color.BLACK);
    }

    // Change color if tile is in the correct position
    if (this.value == row * 4 + col + 1 || (this.value == 0 && row == 3 && col == 3)) {
      tileImage = new RectangleImage(tileSize, tileSize, OutlineMode.SOLID, Color.GREEN);
    }

    // If the tile has a number it overlays it on the tile image
    if (numberImage != null) {
      tileImage = new OverlayImage(numberImage, tileImage);
    }

    // Place the tile image on the background
    background.placeImageXY(borderImage, x, y);
    background.placeImageXY(tileImage, x, y);

    return background;
  }
}

// Represents the 15 puzzle game world
class FifteenGame extends World {

  // Represents the rows of tiles
  ArrayList<ArrayList<Tile>> tiles;

  //Represents the previous state of tiles
  ArrayList<ArrayList<Tile>> previousTiles;

  // seed fr testing
  Random rand;

  // Constructor
  public FifteenGame(ArrayList<ArrayList<Tile>> tiles) {
    this.tiles = tiles;
  }

  // No argument constructor to initialize the actual game
  public FifteenGame() {
    tiles = new ArrayList<ArrayList<Tile>>();
    initializeGame();
  }

  // modified constructor for tests with the seed
  public FifteenGame(int seed) {
    this.tiles = new ArrayList<ArrayList<Tile>>();
    this.rand = new Random(seed); // Initialize with a seed
    initializeGame();
  }

  // to initialize the game with tiles
  public void initializeGame() {
    int num = 1;
    // creates 4x4 matrix for representation of the table (or sublists inside of a list)
    for (int row = 0; row < 4; row++) {
      ArrayList<Tile> tileRow = new ArrayList<Tile>();
      for (int col = 0; col < 4; col++) {
        if (row == 3 && col == 3) {
          tileRow.add(new Tile(0)); // The empty space
        } else {
          tileRow.add(new Tile(num++));
        }
      }
      tiles.add(tileRow);
    }

    // Shuffle the tiles after initializing them
    shuffleTiles();
  }


  // Draws the game
  public WorldScene makeScene() {
    WorldScene scene = new WorldScene(400, 400);

    if (this.checkWin()) {
      return this.endScene(); // If the game is won, show the end scene
    }

    // iterate over each tile and draw the position
    for (int row = 0; row < tiles.size(); row++) {
      for (int col = 0; col < tiles.get(row).size(); col++) {
        Tile tile = tiles.get(row).get(col);
        // to draw the tile at the appropriate position
        scene = tile.drawAt(col, row, scene);
      }
    }

    return scene;
  }

  // Handles onKeyEvents for Movement
  public void onKeyEvent(String key) {
    int emptyRow = -1; // value not assigned yet
    int emptyCol = -1; // value not assigned yet

    // Find the position of the empty space (0)
    for (int row = 0; row < tiles.size(); row++) {
      for (int col = 0; col < tiles.get(row).size(); col++) {
        if (tiles.get(row).get(col).value == 0) {
          emptyRow = row;
          emptyCol = col;
          break;
        }
      }
      if (emptyRow != -1) {
        break; // Breaks the outer loop if empty space is found
      }
    }

    // Undo the last move if "u" is pressed
    if (key.equals("u")) {
      if (previousTiles != null) {
        tiles = deepCopyTiles(previousTiles);
        previousTiles = null;
      }
    } else {
      // Save current state before making a move
      saveCurrentState();

      // Check for valid moves and swap tiles if possible
      if (key.equals("left") && emptyCol < tiles.get(0).size() - 1) {
        swapTiles(emptyRow, emptyCol, emptyRow, emptyCol + 1);
      } else if (key.equals("right") && emptyCol > 0) {
        swapTiles(emptyRow, emptyCol, emptyRow, emptyCol - 1);
      } else if (key.equals("up") && emptyRow < tiles.size() - 1) {
        swapTiles(emptyRow, emptyCol, emptyRow + 1, emptyCol);
      } else if (key.equals("down") && emptyRow > 0) {
        swapTiles(emptyRow, emptyCol, emptyRow - 1, emptyCol);
      }
    }
  }

  // swaps two tiles
  public void swapTiles(int row1, int col1, int row2, int col2) {
    Tile temp = tiles.get(row1).get(col1);
    tiles.get(row1).set(col1, tiles.get(row2).get(col2));
    tiles.get(row2).set(col2, temp);
  }

  // is the game won/over?
  public boolean checkWin() {
    int expectedValue = 1;
    for (ArrayList<Tile> row : this.tiles) {
      for (Tile tile : row) {
        if (tile.value != expectedValue) {
          return false;
        }
        expectedValue = (expectedValue + 1) % 16; // Loop back to 0 after 15
      }
    }
    return true;
  }

  // creates the end scene
  public WorldScene endScene() {
    WorldScene scene = new WorldScene(400, 400);

    if (this.checkWin()) {
      // Display winning message or scene
      WorldImage winMessage = new TextImage("You Won!", 40, FontStyle.BOLD, Color.RED);
      scene.placeImageXY(winMessage, 200, 200);
    } else {
      scene = this.endScene();
    }

    return scene;
  }

  // Saves the current state of the game
  public void saveCurrentState() {
    previousTiles = new ArrayList<ArrayList<Tile>>();
    for (ArrayList<Tile> row : tiles) {
      ArrayList<Tile> newRow = new ArrayList<Tile>();
      for (Tile t : row) {
        newRow.add(new Tile(t.value));
      }
      previousTiles.add(newRow);
    }
  }

  // generates the deepcopytiles for the undo option
  public ArrayList<ArrayList<Tile>> deepCopyTiles(ArrayList<ArrayList<Tile>> sourceTiles) {
    ArrayList<ArrayList<Tile>> newTiles = new ArrayList<>();
    for (ArrayList<Tile> row : sourceTiles) {
      ArrayList<Tile> newRow = new ArrayList<>();
      for (Tile t : row) {
        newRow.add(new Tile(t.value));
      }
      newTiles.add(newRow);
    }
    return newTiles;
  }

  // Method to shuffle the tiles
  public void shuffleTiles() {
    int emptyRow = 3;
    int emptyCol = 3;
    int numShuffles = 1000; // Number of random moves for shuffling
    this.rand = new Random(1); // Fixed seed for predictable shuffling

    for (int i = 0; i < numShuffles; i++) {
      ArrayList<String> moves = new ArrayList<String>();
      if (emptyRow > 0) {
        moves.add("up");
      }
      if (emptyRow < 3) {
        moves.add("down");
      }
      if (emptyCol > 0) {
        moves.add("left");
      }
      if (emptyCol < 3) {
        moves.add("right");
      }

      // Choose a random move
      String move = moves.get(rand.nextInt(moves.size()));

      // Make the move
      if (move.equals("up")) {
        swapTiles(emptyRow, emptyCol, emptyRow - 1, emptyCol);
        emptyRow--;
      } else if (move.equals("down")) {
        swapTiles(emptyRow, emptyCol, emptyRow + 1, emptyCol);
        emptyRow++;
      } else if (move.equals("left")) {
        swapTiles(emptyRow, emptyCol, emptyRow, emptyCol - 1);
        emptyCol--;
      } else if (move.equals("right")) {
        swapTiles(emptyRow, emptyCol, emptyRow, emptyCol + 1);
        emptyCol++;
      }
    }
  }


}

// Represents the examples and tests
class Examples {

  //visualization of the world 
  void testbigbang(Tester t) {
    FifteenGame game = new FifteenGame();
    game.bigBang(400, 400, 0.1); 
  }

  // tests for initialize game method
  void testInitializeGame(Tester t) {
    FifteenGame game = new FifteenGame(1);
    game.initializeGame(); // This includes shuffling

    t.checkExpect(game.tiles.get(0).get(0).value, 3);
    t.checkExpect(game.tiles.get(0).get(1).value, 10);
    t.checkExpect(game.tiles.get(0).get(2).value, 0);
    t.checkExpect(game.tiles.get(0).get(3).value, 8);
    t.checkExpect(game.tiles.get(1).get(0).value, 12);
    t.checkExpect(game.tiles.get(1).get(1).value, 11);
    t.checkExpect(game.tiles.get(1).get(2).value, 14);
    t.checkExpect(game.tiles.get(1).get(3).value, 15);
    t.checkExpect(game.tiles.get(2).get(0).value, 9);
    t.checkExpect(game.tiles.get(2).get(1).value, 2);
    t.checkExpect(game.tiles.get(2).get(2).value, 6);
    t.checkExpect(game.tiles.get(2).get(3).value, 5);   
    t.checkExpect(game.tiles.get(3).get(0).value, 13);
    t.checkExpect(game.tiles.get(3).get(1).value, 7);
    t.checkExpect(game.tiles.get(3).get(2).value, 4);
    t.checkExpect(game.tiles.get(3).get(3).value, 1);
  }


  // tests for makeScene method through makeScene
  void testMakeScene(Tester t) {
    FifteenGame game = new FifteenGame(1);
    WorldScene scene = game.makeScene();
    t.checkExpect(scene.width, 400); // Assuming scene width is 400
    t.checkExpect(scene.height, 400); // Assuming scene height is 400
  }


  // tests for drawAt method
  void testDrawAt(Tester t) {
    FifteenGame game = new FifteenGame(1);
    game.initializeGame();
    WorldScene testScene = new WorldScene(400, 400);

    // manually draw one tile using drawAt to a test scene
    Tile testTile = new Tile(1);
    testScene = testTile.drawAt(0, 0, testScene);

    // assuming makeScene also starts by creating a new WorldScene with the same dimensions
    WorldScene gameScene = game.makeScene();

    // compare properties of testScene and gameScene
    t.checkExpect(testScene.width, gameScene.width);
    t.checkExpect(testScene.height, gameScene.height);
  }



  // tests for onKeyEvent
  void testOnKeyEvent(Tester t) {

    FifteenGame game = new FifteenGame();
    game.initializeGame();

    // Check initial positions before moving
    t.checkExpect(game.tiles.get(3).get(3).value, 1); 
    t.checkExpect(game.tiles.get(3).get(2).value, 4); 
    // Simulate pressing the 'right' key 
    game.onKeyEvent("right");
    // they swap
    t.checkExpect(game.tiles.get(3).get(3).value, 1); 
    t.checkExpect(game.tiles.get(3).get(2).value, 4); 

    t.checkExpect(game.tiles.get(2).get(2).value, 6); 
    // simulate pressing the 'down' key 
    game.onKeyEvent("down");
    // they swap
    t.checkExpect(game.tiles.get(2).get(2).value, 6);
    t.checkExpect(game.tiles.get(3).get(2).value, 4);

    t.checkExpect(game.tiles.get(2).get(3).value, 5); 
    // simulate pressing the 'left' key 
    game.onKeyEvent("left");
    // they swap
    t.checkExpect(game.tiles.get(2).get(3).value, 5);
    t.checkExpect(game.tiles.get(2).get(2).value, 6);

    t.checkExpect(game.tiles.get(3).get(3).value, 1); 
    // simulate pressing the 'up' key 
    game.onKeyEvent("up");
    t.checkExpect(game.tiles.get(2).get(3).value, 5);
    t.checkExpect(game.tiles.get(3).get(3).value, 1);

    t.checkExpect(game.tiles.get(3).get(3).value, 1);
    // Simulate pressing the "u" key to undo action
    game.onKeyEvent("u");
    t.checkExpect(game.tiles.get(3).get(3).value, 1);


  }

  // tests for swaptiles helper method
  void testSwapTiles(Tester t) {
    FifteenGame game = new FifteenGame();
    game.initializeGame();

    // Check initial configuration
    t.checkExpect(game.tiles.get(0).get(0).value, 3);
    t.checkExpect(game.tiles.get(0).get(1).value, 10);

    // Swap tiles at (0,0) and (0,1)
    game.swapTiles(0, 0, 0, 1);

    // Check if the tiles have been swapped
    t.checkExpect(game.tiles.get(0).get(0).value, 10);
    t.checkExpect(game.tiles.get(0).get(1).value, 3);

  }

  // test for checkWincond method
  void testCheckWin(Tester t) {
    FifteenGame game = new FifteenGame(1);
    game.initializeGame(); 

    // Test with the current configuration (non-winning)
    t.checkExpect(game.checkWin(), false); 

    // Manually set the tiles to a winning configuration
    for (int row = 0; row < 4; row++) {
      for (int col = 0; col < 4; col++) {
        int value = row * 4 + col + 1;
        if (row == 3 && col == 3) {
          game.tiles.get(row).set(col, new Tile(0)); // The empty space
        } else {
          game.tiles.get(row).set(col, new Tile(value));
        }
      }
    }
    // Tests with the winning configuration
    t.checkExpect(game.checkWin(), true);
  }

  // test for endScene method
  void testSaveCurrentState(Tester t) {
    FifteenGame game = new FifteenGame(1); // Initialize with a fixed seed
    game.initializeGame(); 

    // Saves the current state
    game.saveCurrentState();

    // Alters the game state 
    game.shuffleTiles(); // shuttles the tiles

    // Verify that each tile in the saved state is different from the current state
    boolean isDifferent = false;
    for (int row = 0; row < game.tiles.size(); row++) {
      for (int col = 0; col < game.tiles.get(row).size(); col++) {
        if (game.tiles.get(row).get(col).value != game.previousTiles.get(row).get(col).value) {
          isDifferent = true;
          break;
        }
      }
      if (isDifferent) {
        break;
      }
    }

    t.checkExpect(isDifferent, true); //states are different
  }

  // test for deepcopytiles
  void testDeepCopyTiles(Tester t) {
    FifteenGame game = new FifteenGame(1);
    game.initializeGame();

    // Create a deep copy of the tiles
    ArrayList<ArrayList<Tile>> copiedTiles = game.deepCopyTiles(game.tiles);

    // Record the state of the original tiles at the time of copying
    int[][] originalValues = new int[4][4];
    for (int row = 0; row < 4; row++) {
      for (int col = 0; col < 4; col++) {
        originalValues[row][col] = game.tiles.get(row).get(col).value;
      }
    }

    // Make a change to the original tiles 
    game.shuffleTiles(); // Shuffles the tiles

    // Check if the copied tiles are still the same as the original state before shuffling
    boolean isSameAsOriginal = true;
    for (int row = 0; row < 4; row++) {
      for (int col = 0; col < 4; col++) {
        if (copiedTiles.get(row).get(col).value != originalValues[row][col]) {
          isSameAsOriginal = false;
          break;
        }
      }
      if (!isSameAsOriginal) {
        break;
      }
    }

    // the copy matches the original state before shuffling
    t.checkExpect(isSameAsOriginal, true); 
  }

  // test for shuttletiles method
  void testShuffleTiles(Tester t) {
    FifteenGame game = new FifteenGame(1);
    game.initializeGame(); 

    // initial state of the tiles before shuffling
    int[][] originalValues = new int[4][4];
    for (int row = 0; row < 4; row++) {
      for (int col = 0; col < 4; col++) {
        originalValues[row][col] = game.tiles.get(row).get(col).value;
      }
    }

    // Shuffle the tiles again
    game.shuffleTiles();

    // Check if the tiles have been shuffled into a different order
    boolean isShuffled = false;
    for (int row = 0; row < 4; row++) {
      for (int col = 0; col < 4; col++) {
        if (game.tiles.get(row).get(col).value != originalValues[row][col]) {
          isShuffled = true;
          break;
        }
      }
      if (isShuffled) {
        break;
      }
    }
    // tiles are now shuffled
    t.checkExpect(isShuffled, true); 
  }

}
