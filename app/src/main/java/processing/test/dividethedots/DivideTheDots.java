package processing.test.dividethedots;

import processing.core.*; 
import processing.data.*; 
import processing.event.*; 
import processing.opengl.*; 

import java.util.LinkedList; 

import java.util.HashMap; 
import java.util.ArrayList; 
import java.io.File; 
import java.io.BufferedReader; 
import java.io.PrintWriter; 
import java.io.InputStream; 
import java.io.OutputStream; 
import java.io.IOException; 

public class DivideTheDots extends PApplet {



/**
 * A game where the user moves the cursor to divide the single starting dot into successivly smaller dots. 
 * The dots act like pixels to reveal a picture.
 */

/**
 * The maximum number of times a dot can be divided.
 */
private static final int MAX_DOT_DIVISIONS = 9; 

/**
 * The minimal radius of a dot, in pixels.
 */
private float MIN_DOT_SIZE;

/**
 * The smallest radius a dot can have before the program makes it easier to clear.
 */
private static float MIN_UNASSISTED_RADIUS; 


/**
 * All the Dots on the screen that are greater than MIN_DOT_SIZE.
 */
private LinkedList<Dot> dots;

/**
 * All the AnimatedSections currently being animated (drawn each frame).
 */
private LinkedList<AnimatedSection> sections;

/**
 * The background image that will be revealed.
 */
static PImage photo;

/**
 * A vector from the cursor position in the previous frame to the current cursor position.
 * Linearly approximates the path the cursor took between frames.
 */
PVector cursorPath;

/**
 * Sets up the program, runs at program start-up / reset.
 */
public void setup() {
  ////////////////////////////////////
  //  CHANGE BACKGROUND IMAGE HERE  //
  //                                //
  // If you would like a non-random //
  // image, add the file-name of    //
  // the image in quotes as an      //
  // argument to the setupPhoto()   //
  // call below. For example,       //
  // setupPhoto("example.jpg")      //
  // If you would like a random     //
  // image, use the empty string    //
  // as the argument.               //
  //                                //
  // NOTE: If you use your own      //
  // image, place the image in the  //
  // data folder. The image should  //
  // be square or it will appear    //
  // distorted.                     //
  ////////////////////////////////////
  setupPhoto(""); //
  ////////////////////////////////////
  
  // Cannot use variables in size(), assuming displayHeight < dislayWidth;
   
  photo.resize(width, height);
  // Update the pixels[] array for photo
  photo.loadPixels();
  
  frameRate(60);
  noStroke();
  ellipseMode(RADIUS);
  rectMode(CENTER);
  int minHeightWidth = min(height, width);
  
  MIN_DOT_SIZE = minHeightWidth / pow(2, MAX_DOT_DIVISIONS);
  MIN_UNASSISTED_RADIUS = minHeightWidth / pow(2, 7);

  sections = new LinkedList<AnimatedSection>();
  dots = new LinkedList<Dot>();
  Dot firstDot = new Dot(width / 2, height / 2, height / 2);
  dots.add(firstDot);

  background(0);
  firstDot.drawDot();
}

private void setupPhoto(String photoName) {
  boolean validName = false;
  String[] photoNames = loadStrings("fileNames.txt");
  for (String name : photoNames) {
    if (name.equals(photoName)) {
      validName = true;
      break;
    }
  }
  if (!validName) {
    photoName = photoNames[(int) random(photoNames.length)];
  }
  photo = loadImage(photoName);
}

/**
 * Runs frameRate times per second. If cursor has moved then iterate through the dots on the screen and divide them as necessary.
 */
public void draw() {
  drawSections();
  cursorPath = new PVector(mouseX - pmouseX, mouseY - pmouseY);
  // If the cursor has moved. 
  if (cursorPath.magSq() >= 1) { 
    LinkedList<Dot> tempDotsToRemove = new LinkedList<Dot>();
    for (Dot dot : dots) {
      float radius = dot.getRadius();
      // dot should only divide if cursor starts outside of dot and then enters it.
      boolean pathStartsOutsideDot = dist(dot.getX(), dot.getY(), pmouseX, pmouseY) > radius; 
      if (pathStartsOutsideDot && pathIntersectsDot(dot)) {
        sections.add(new AnimatedSection(dot));
        tempDotsToRemove.add(dot);
      }
    }
    dots.removeAll(tempDotsToRemove);
  }
}

public void drawSections() {
  LinkedList<AnimatedSection> tempSectionsToRemove = new LinkedList<AnimatedSection>();

  for (AnimatedSection section : sections) {
    if (section.drawSection()) {
      Dot[] dotsCreated = section.getDotsCreated();
      // All dots in section same size, if Dot is min size, don't add the Dots from this 
      // section to dots because we don't want to divide them anymore.
      if (dotsCreated[0].getRadius() > MIN_DOT_SIZE) { 
        for (Dot dot : dotsCreated) {
          dots.add(dot);
        }
      }
      tempSectionsToRemove.add(section);
    }
  }
  sections.removeAll(tempSectionsToRemove);
}

/**
 * Return the minimal distance from the center of dot to anywhere on the cursorPath vector.
 * 
 * @param dot The dot which we are computing the distance to.
 * @return the minimal distance from the center of dot to anywhere on the cursorPath vector.
 */
private float distToCursorPath(Dot dot) {
  PVector oldCursorToDot = new PVector(dot.getX() - pmouseX, dot.getY() - pmouseY); // Vector from old cursor position to center of dot.
  PVector currentCursorToDot = new PVector(dot.getX() - mouseX, dot.getY() - mouseY); // Vector from current cursor position to center of dot.
  PVector reverseCursorPath = PVector.mult(cursorPath, -1); // Vector from the current cursor position to the old cursor position.

  // Case (1)
  float angleOldToCurrent_OldToDot = PVector.angleBetween(cursorPath, oldCursorToDot);
  /*
   If the angle between these vectors is obtuse, then the dot lies behind the cursorPath 
   vector and the point on the cursorPath vector which is closest to dot is the starting 
   point; the old cursor position.
   */
  if (angleOldToCurrent_OldToDot > HALF_PI) {
    return dist(dot.getX(), dot.getY(), pmouseX, pmouseY);
  }

  // Case (2)
  float angleCurrentToOld_CurrentToDot = PVector.angleBetween(reverseCursorPath, currentCursorToDot);
  /*
   If the angle between these vectors is obtuse, then the dot lies in front of the cursorPath 
   vector and the point on the cursorPath vector which is closest to dot is the ending point; the 
   current cursor position.
   */
  if (angleCurrentToOld_CurrentToDot > HALF_PI) {
    return dist(dot.getX(), dot.getY(), mouseX, mouseY);
  }

  // Case (3)
  /* 
   If both of the above angles are acute, then the dot is between the endpoints of the cursorPath vector and the 
   point on the path which is closest to the dot is somewhere along the path and not one of the endpoints. The 
   magnitude of vector perpendicular to cursorPath with endpoints at the intersection points of dot and cursorPath 
   is the minimal distance from dot to cursorPath. We calculate this by forming a right triangle and using trigonometry.
   */
  return sin(angleOldToCurrent_OldToDot) * oldCursorToDot.mag(); // Angles always positive for non-zero vectors -> sin(angle) is positive.
}

public boolean pathIntersectsDot(Dot dot) {
  float maxDistance;
  float radius = dot.getRadius();
  if (radius < MIN_UNASSISTED_RADIUS) {
    // It's annoyingly difficult to clear the very small dots, so we make it slightly more forgiving.
    maxDistance =  radius * 1.4f;
  } else {
    maxDistance = radius;
  }
  return distToCursorPath(dot) <= maxDistance;
}

public void keyPressed() {
  if (key == ' ' || key == 'n' || key == 'r') {
    setup();
    println("reset");
  }
}
class AnimatedSection {
  private float x;
  private float y;
  private float sideLength;
  private Dot[] animatedDots;

  AnimatedSection(Dot dot) {
    x = dot.getX();
    y = dot.getY();
    sideLength = dot.getRadius() * 2;
    
    float halfOldRadius = sideLength / 4;
    Dot topLeft = new Dot(x - halfOldRadius, y - halfOldRadius, halfOldRadius);
    Dot topRight = new Dot(x + halfOldRadius, y - halfOldRadius, halfOldRadius);
    Dot bottomLeft = new Dot(x - halfOldRadius, y + halfOldRadius, halfOldRadius);
    Dot bottomRight = new Dot(x + halfOldRadius, y + halfOldRadius, halfOldRadius);
    animatedDots = new Dot[] {topLeft, topRight, bottomLeft, bottomRight};
  }
  
  public boolean drawSection() {
    fill(0);
    rect(x, y, sideLength, sideLength);
    boolean doneAnimating = true;
    for (Dot dot : animatedDots) {
      if(!dot.drawDot(x, y)) {
        doneAnimating = false;
      }
    }
    return doneAnimating;
  }
  
  public Dot[] getDotsCreated() {
    return animatedDots;
  }
  
}
/**
 * A Dot; represented as a single circle on-screen.
 */
private class Dot {

  /**
   * The x position of the center of the Dot.
   */
  private float x;

  /**
   * The y position of the center of the Dot.
   */
  private float y;

  /**
   * The radius of the Dot, in pixels.
   */
  private float radius;

  /**
   * The fill color of the Dot.
   */
  private int dotColor;

  /**
   * The most recent time that the Dot was divided, in milliseconds since the program started.
   */
  private long timeLastDivided;

  /**
   * The minimal amount of time between divisions, in milliseconds. Prevents the appearence of rapid multiple-division.
   */
  public static final int ANIMATION_TIME = 210;  

  /**
   * Create a new Dot.
   *
   * @param x            The x position of the center of the Dot.
   * @param y            The y position of the center of the Dot.
   * @param radius       The radius of the Dot, in pixels.
   */
  Dot(float x, float y, float radius) {
    this.x = x;
    this.y = y;
    this.radius = radius;
    calculateColor();
    timeLastDivided = millis();
  }
  
  /**
   * Return the x position of the center of the Dot.
   * @return The x position of the center of the Dot.
   */
  public float getX() {
    return x;
  }

  /**
   * Return the y position of the center of the Dot.
   * @return The y position of the center of the Dot.
   */
  public float getY() {
    return y;
  }

  /**
   * Return the radius of the Dot.
   * @return The radius of the Dot.
   */
  public float getRadius() {
    return radius;
  }

  /**
   * Draw the Dot to the screen with a radius and position based on how long 
   * the Dot has been animataing. Return True iff dot is done animating. 
   * Important we check this at time of drawing and not before or after 
   * so we do not get innacurate results due to imprecise timing.
   *
   * @param oldDotX The x position of the Dot that was divided to create this Dot.
   * @param oldDotY The y position of the Dot that was divided to create this Dot.
   * @return true iff the dot is finished animating.
   */
  public boolean drawDot(float oldDotX, float oldDotY) {
    fill(dotColor);
    long timeSinceDivision = millis() - timeLastDivided;
    if (timeSinceDivision >= ANIMATION_TIME) {
      ellipse(x, y, radius, radius);
      return true;
    }
    float percentComplete = (float) timeSinceDivision / ANIMATION_TIME;
    float xDiff = x - oldDotX;
    float yDiff = y - oldDotY;
    float rDiff = -radius; //radius - oldRadius = radius - 2*radius = -radius
    float displayX = oldDotX + (percentComplete * xDiff);
    float displayY = oldDotY + (percentComplete * yDiff);
    float displayR = (2 * radius) + (percentComplete * rDiff);
    ellipse(displayX, displayY, displayR, displayR);

    return false;
  }

  /**
   * Draw the Dot to the screen, unanimated, at position x, y with radius radius. 
   */
  public void drawDot() {
    fill(dotColor);
    ellipse(x, y, radius, radius);
  }

  /**
   * Caculate the value of dotColor; the fill color of the Dot. Accomplishes this by 
   * averaging the colors of the pixels of photo which are overapped by the square 
   * centered at (x,y) with side length 2*radius. This is the square centered around 
   * the Dot and of minimal size such that it still contains the Dot.
   */
  private void calculateColor() {
    // Accumulated r, g, b values 
    float r = 0;
    float g = 0;
    float b = 0;


    int rRadius = round(radius);
    int rx = round(x);
    int ry = round(y);

    // Iterate over the pixels of photo which are overapped by the square 
    // centered at (rx,ry) with side length 2 * rRadius
    for (int i = 0; i < 2 * rRadius; i++) {
      for (int j = 0; j < 2 * rRadius; j++) {
        // photo.get(0,0) is the top left corner
        r += red(photo.get(rx - rRadius + i, ry - rRadius + j));
        g += green(photo.get(rx - rRadius + i, ry - rRadius + j));
        b += blue(photo.get(rx - rRadius + i, ry - rRadius + j));
      }
    }
    // Take mean.
    int numIterations = (4*rRadius*rRadius);
    r /= numIterations;
    g /= numIterations;
    b /= numIterations;
    this.dotColor = color(r, g, b);
  }
}
  public void settings() {  size(displayWidth, displayWidth); }
}
