package com.horowitz.mickey.trainScanner;

import java.awt.AWTException;
import java.awt.Rectangle;
import java.awt.Robot;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import javax.imageio.ImageIO;

import com.horowitz.mickey.DateUtils;
import com.horowitz.mickey.ImageComparator;
import com.horowitz.mickey.ImageData;
import com.horowitz.mickey.ImageManager;
import com.horowitz.mickey.MouseRobot;
import com.horowitz.mickey.Pixel;
import com.horowitz.mickey.RobotInterruptedException;
import com.horowitz.mickey.ScreenScanner;
import com.horowitz.mickey.data.DataStore;

public class TrainScanner {

  private boolean         _locoOnly;
  private ScreenScanner   _scanner;
  private ImageComparator _comparator;
  private MouseRobot      _mouse;
  Logger                  LOGGER;

  public TrainScanner(ScreenScanner scanner, Logger logger) {
    super();
    _scanner = scanner;
    _comparator = scanner.getComparator();
    try {
      _mouse = new MouseRobot();
    } catch (AWTException e) {
    }
    LOGGER = logger;
  }

  public List<Train> analyzeIntTrains(boolean all) {
    List<Train> trains = null;
    try {
      int xx = (_scanner.getGameWidth() - 759) / 2;
      int yy = (_scanner.getGameHeight() - 550) / 2;
      xx += _scanner.getTopLeft().x;
      yy += _scanner.getTopLeft().y;

      _mouse.click(_scanner.getTopLeft().x + 56, _scanner.getTopLeft().y + 72);
      _mouse.delay(1300);
      _mouse.click(xx + 127, yy + 101);
      _mouse.delay(2300);
      _mouse.mouseMove(_scanner.getBottomRight());

      // make sure it is trains and it is international
      Pixel p = _scanner.getTrainsAnchor().findImage();
      if (p != null) {
        int x = p.x - 333;
        int y = p.y - 37;
        LOGGER.info("Scanning int. trains...");

        int xt = 0;
        int yt = y + 126;
        ImageData upArrow = _scanner.generateImageData("int/Up.bmp");
        boolean hasScroller = upArrow.findImage(new Rectangle(x + 697, y + 116, 43, 58)) != null;
        xt = x + (hasScroller ? 26 : 41);
        // trains = scanIntTrains(x, y, xt, yt, hasScroller);
        trains = scanIntTrains(x, y, xt, yt, hasScroller, all);
        // trains = scanIntTrains(x, y, xt, yt, false);

      } else {
        System.err.println("can't find trains anchor");
      }

    } catch (RobotInterruptedException | IOException | AWTException e) {
      LOGGER.severe("Scanning interrupted! The data is incomplete!!!");
      e.printStackTrace();
    } finally {
      closeTrains();
    }
    return trains;
  }

  private List<Train> scanIntTrains(int x, int y, int xt, int yt, boolean hasScroller, boolean all) throws RobotInterruptedException, IOException,
      AWTException {
    List<Train> trains = new ArrayList<>();
    if (hasScroller) {
      _mouse.mouseMove(x + 719, y + 157);// center of scrollball placed in the beginning
      _mouse.click();
      _mouse.delay(1000);
    }

    yt += 63; // skip first slot
    xt += 3;

    scanSlots(xt, yt, 1, trains, all);
    if (hasScroller) {
      pageDown(x, y, 2);
      _mouse.delay(2000);
      scanSlots(xt, yt, 2, trains, all);
      _mouse.delay(1000);

      pageDown(x, y, 3);
      _mouse.delay(2000);
      scanSlots(xt, yt, 3, trains, all);
      _mouse.delay(1000);

      LOGGER.info("DONE");
    }
    return trains;
  }

  private void scanSlots(int xt, int yt, int page, List<Train> trains, boolean all) throws AWTException, IOException, RobotInterruptedException {
    for (int slot = 0; slot < 5; slot++) {
      _mouse.delay(400);
      Rectangle slotArea = new Rectangle(xt, yt + slot * 60, 666, 50);
      Rectangle delArea = new Rectangle(slotArea.x, slotArea.y + 24, 50, 20);
      ImageData delData = _scanner.generateImageData("int/Del.bmp");
      boolean isIdle = delData.findImage(delArea) == null;
      if (all || isIdle) {
        int number = slot + 1 + ((page - 1) * 5);
        String trainId = number + "  " + DateUtils.formatDateForFile(System.currentTimeMillis());
        String fullImageFilename = "data/int/train " + trainId + ".png";
        writeImage(slotArea, fullImageFilename);
        Rectangle newArea = new Rectangle(slotArea.x + 148, slotArea.y + 2, 512, 24);
        String scanImageFilename = "data/int/train " + trainId + "_scanThis.bmp";
        writeImage(newArea, scanImageFilename);
        Robot robot = new Robot();
        Train train = new Train(robot.createScreenCapture(slotArea), robot.createScreenCapture(newArea));
        train.setFullImageFilename(fullImageFilename);
        train.setScanImageFilename(scanImageFilename);

        if (!isIdle) {
          LOGGER.info("Found Del.bmp in slot " + slot);
          // scan additional info
          _mouse.savePosition();
          _mouse.mouseMove(slotArea.x + 9, slotArea.y + 9);
          _mouse.delay(800);
          Rectangle infoArea = new Rectangle(slotArea.x, slotArea.y + slotArea.height, 590, 110);
          String addInfoFilename = "data/int/trainInfo " + trainId + ".bmp";
          writeImage(infoArea, addInfoFilename);
          train.setAdditionalInfo(cutToEdge(robot.createScreenCapture(infoArea)));

          train.setAdditionalInfoFilename(addInfoFilename);

          // short info
          ImageData westEnd = _scanner.generateImageData("int/westEnd.bmp");
          ImageData westEnd2 = _scanner.generateImageData("int/westEnd2.bmp");
          Pixel pwest = westEnd.findImage(train.getAdditionalInfo().getSubimage(60, 0, 64, train.getAdditionalInfo().getHeight()));
          if (pwest == null) {
            pwest = westEnd2.findImage(train.getAdditionalInfo().getSubimage(60, 0, 64, train.getAdditionalInfo().getHeight()));
          } else {
            System.err.println("found westEnd");
          }
          if (pwest == null) {
            pwest = new Pixel(13, 0);
          } else {
            System.err.println("found westEnd2");
          }

          System.err.println(pwest);

          train.setAdditionalInfoShort(train.getAdditionalInfo().getSubimage(0, 0, 60 + pwest.x - 1, train.getAdditionalInfo().getHeight()));

          String shortFilename = "data/int/trainInfo " + trainId + "_short.png";
          _scanner.writeImage(train.getAdditionalInfoShort(), shortFilename);
          train.setAdditionalInfoShortFilename(shortFilename);
          train.setIdle(false);

          List<String> contractorNames = scanContractorsSimple(train);

          train.setContractors(contractorNames);
          train.setTimeToSendNext(0l);// TODO ocr capture time and set it
          _mouse.restorePosition();
        } else {
          train.setContractors(new ArrayList<String>());
          train.setIdle(true);
          train.setTimeToSendNext(0l);
        }
        trains.add(train);
      }// if all or is idle
    }
  }

  private BufferedImage cutToEdge(BufferedImage image) {
    BufferedImage result = image;
    try {
      BufferedImage edgeImage = ImageIO.read(ImageManager.getImageURL("int/brEdge.bmp"));
      Pixel p = _comparator.findImage(edgeImage, image);
      if (p != null) {
        System.err.println("found edge " + p);
        result = image.getSubimage(0, 0, p.x + edgeImage.getWidth(), p.y + edgeImage.getHeight());
      }
    } catch (IOException e) {
    }
    return result;
  }

  private void pageDown(int x, int y, int page) {
    Pixel p = new Pixel(x + 719, y + 157);
    System.err.println("Page: " + page);
    LOGGER.info("Page: " + page);
    _mouse.mouseMove(p.x, p.y + (147 * (page - 1)));
    try {
      _mouse.delay(200);
      _mouse.click();
      _mouse.delay(300);
      _mouse.click();
      _mouse.delay(500);
      _mouse.click();
      _mouse.delay(500);
    } catch (RobotInterruptedException e) {
    }

  }

  public boolean sendTrains(List<Train> trains) {
    boolean atLeastOneSent = false;
    int x = 0;
    int y = 0;
    try {
      int xx = (_scanner.getGameWidth() - 759) / 2;
      int yy = (_scanner.getGameHeight() - 550) / 2;
      xx += _scanner.getTopLeft().x;
      yy += _scanner.getTopLeft().y;

      _mouse.click(_scanner.getTopLeft().x + 56, _scanner.getTopLeft().y + 72);
      _mouse.delay(1300, false);
      _mouse.click(xx + 127, yy + 101);
      _mouse.delay(2300, false);
      _mouse.mouseMove(_scanner.getBottomRight());

      // make sure it is trains and it is international
      Pixel p = _scanner.getTrainsAnchor().findImage();
      if (p != null) {
        x = p.x - 333;
        y = p.y - 37;
        LOGGER.info("Sending international...");

        int xt = 0;
        int yt = y + 126;
        ImageData upArrow = _scanner.generateImageData("int/Up.bmp");
        boolean hasScroller = upArrow.findImage(new Rectangle(x + 697, y + 116, 43, 58)) != null;
        xt = x + (hasScroller ? 26 : 41);
        atLeastOneSent = sendIntTrains(trains, x, y, xt, yt, hasScroller);

      } else {
        System.err.println("can't find trains anchor");
        LOGGER.severe("Trains not opened!");
      }
    } catch (IOException | AWTException e) {
      LOGGER.severe("Ooops. Something went wrong!");
      e.printStackTrace();
      closeTrains();
    } catch (RobotInterruptedException e) {
      LOGGER.severe("Interrupted by user");
      closeTrains();
    } finally {
      if (x != 0)
        _mouse.click(x + 159, y + 524);// close the window
    }

    return atLeastOneSent;
  }

  private void closeTrains() {
    Pixel p = _scanner.getTrainsAnchor().findImage();
    if (p != null) {
      int x = p.x - 333;
      int y = p.y - 37;
      _mouse.mouseMove(x + 159, y + 524);
      try {
        _mouse.delay(250, false);
      } catch (RobotInterruptedException e) {
      }
      _mouse.click();
      try {
        _mouse.delay(750);
      } catch (RobotInterruptedException e) {
      }
    }

  }

  private boolean sendIntTrains(List<Train> trains, int x, int y, int xt, int yt, boolean hasScroller) throws RobotInterruptedException, IOException,
      AWTException {
    if (hasScroller) {
      _mouse.mouseMove(x + 719, y + 157);// center of scrollball placed in the beginning
      _mouse.click();
      _mouse.delay(1000);
    }

    yt += 63; // skip first slot
    xt += 3;

    int numberSent = 0;
    boolean atLeastOneSent = false;
    do {
      List<Train> newTrains = scanSlotsForCompare(xt, yt, 1);
      atLeastOneSent = false;
      for (int i = newTrains.size() - 1; i >= 0 && !atLeastOneSent; --i) {
        Train t = newTrains.get(i);
        if (t.isIdle()) {
          for (Train train : trains) {
            long now = System.currentTimeMillis();
            if (!train.getContractors().isEmpty()) {// obsolete && train.getSentTime() - now <= 0
              Pixel p = _comparator.findImage(train.getScanImage(), t.getScanImage());
              if (p == null && isLocoOnly()) {
                BufferedImage i1 = train.getScanImage();
                BufferedImage i2 = t.getScanImage();
                p = _comparator.findImage(i1.getSubimage(i1.getWidth() - 93, 0, 93, i1.getHeight()),
                    i2.getSubimage(i2.getWidth() - 93, 0, 93, i2.getHeight()));
                System.err.println("locoOnly " + p);
              }
              if (p != null) {
                if (sendTrain(train, xt, yt + (i) * 60 + p.y)) {
                  atLeastOneSent = true;
                  numberSent++;
                }
                break;
              }
            }
          } //inner for
        } else {
          LOGGER.info("Train " + (i + 1) + " is not idle");
        }
      } //for newTrains
      System.err.println("...");
    } while (atLeastOneSent);
    return atLeastOneSent;
  }

  private boolean sendTrain(Train train, int x, int y) throws RobotInterruptedException, IOException, AWTException {
    _mouse.mouseMove(x + 72, y + 25);
    _mouse.delay(850);
    _mouse.mouseMove(x + 100, y + 25);
    _mouse.delay(250);
    _mouse.mouseMove(x + 200, y + 25);
    _mouse.click();
    _mouse.delay(650);
    // it is expected a SendDialiog been opened
    int xx = (_scanner.getGameWidth() - 760) / 2;
    int yy = (_scanner.getGameHeight() - 550) / 2;
    xx += _scanner.getTopLeft().x;
    yy += _scanner.getTopLeft().y;

    ImageData selectAnchor = _scanner.generateImageData("int/Select.bmp");
    Pixel p = selectAnchor.findImage(new Rectangle(xx, yy, 200, 75));
    if (p != null) {
      Pixel tl = new Pixel(p.x - 35, p.y - 32);
      try {
        // find and select contractors
        // Rectangle carea = new Rectangle(tl.x + 41, tl.y + 90, 678, 96);
        Rectangle carea = new Rectangle(tl.x + 42, tl.y + 72, 676, 20);
        for (String cname : train.getContractors()) {
          for (int page = 0; page < 3; page++) {
            BufferedImage cimage = new Robot().createScreenCapture(carea);
            // writeImage(carea, "carea.bmp");
            BufferedImage contractorImage = ImageIO.read(ImageManager.getImageURL("int/" + cname + "_name.bmp"));
            Pixel cp = _comparator.findImage(contractorImage, cimage);
            if (cp != null) {
              // found the contractor
              _mouse.mouseMove(carea.x + cp.x + 20, carea.y + cp.y + 50);
              _mouse.delay(450);
              _mouse.click();
              _mouse.delay(950);
              break;
            } else {
              _mouse.delay(350);
              _mouse.click(tl.x + 739, tl.y + 131);
              _mouse.delay(850);
            }
          }
        }
      } catch (Exception e) {
        //no matter what try to send the bloody train
      }

      // TODO click send and choose 4h way
      _mouse.mouseMove(tl.x + 475, tl.y + 517);
      _mouse.delay(300);
      _mouse.click();
      _mouse.delay(700);
      _mouse.click(tl.x + 475, tl.y + 490);
      _mouse.delay(2000);

      //not used
      train.setTimeToSendNext(4 * 60 * 60000 + 60000 + System.currentTimeMillis()); // 4h 1m in the future

      return true;
      // at the end
      // train.setSentTime(System.currentTimeMillis());
    }
    return false;
  }

  private List<Train> scanSlotsForCompare(final int xt, final int yt, final int page) throws AWTException, IOException, RobotInterruptedException {

    List<Train> trains = new ArrayList<>();
    for (int slot = 0; slot < 5; slot++) {
      _mouse.delay(400);
      int number = slot + 1 + ((page - 1) * 5);
      Rectangle slotArea = new Rectangle(xt, yt + slot * 60, 666, 50);
      Rectangle newArea = new Rectangle(slotArea.x + 148, slotArea.y + 2, 512, 24);

      // for debug only
      //String scanImageFilename = "data/int/trainCOMPARE" + number + "_scanThis.bmp";
      //writeImage(newArea, scanImageFilename);
      //

      Robot robot = new Robot();
      Train train = new Train(robot.createScreenCapture(slotArea), robot.createScreenCapture(newArea));

      Rectangle delArea = new Rectangle(slotArea.x, slotArea.y + 24, 50, 20);
      ImageData delData = _scanner.generateImageData("int/Del.bmp");
      train.setIdle(delData.findImage(delArea) == null);

      trains.add(train);
    }
    return trains;
  }

  private List<String> scanContractorsSimple(Train train) {
    List<String> result = new ArrayList<>();

    if (train.getAdditionalInfo() != null) {
      BufferedImage subimage = train.getAdditionalInfo().getSubimage(60, 0, train.getAdditionalInfo().getWidth() - 60,
          train.getAdditionalInfo().getHeight());
      try {
        List<String> activeContractorNames = new DataStore().getActiveContractorNames();
        while (subimage.getWidth() >= 50) {
          boolean atLeastOneFound = false;
          for (String cname : activeContractorNames) {
            if (!result.contains(cname)) {
              String filename = "int/" + cname + "3.bmp";
              BufferedImage cimage = ImageIO.read(ImageManager.getImageURL(filename));
              if (cimage != null) {
                Pixel p = _comparator.findImage(cimage, subimage);
                if (p != null) {
                  atLeastOneFound = true;
                  System.err.println("Found " + cname);
                  subimage = subimage.getSubimage(cimage.getWidth() + 3, 0, subimage.getWidth() - cimage.getWidth() - 3, subimage.getHeight());
                  result.add(cname);
                  break;
                }// if
              }// if cimage != null
            }// if contains
          }// for
          if (!atLeastOneFound) {
            break;
          }
        } // while
      } catch (IOException e) {
        e.printStackTrace();
      }
    }// if
    return result;
  }

  private boolean reachedEnd(int x, int y) {
    try {
      final ImageData scrollerEnded = _scanner.generateImageData("int/scrollerEnded.bmp");
      Rectangle area = new Rectangle(x + 703, y + 431, 32, 53);
      return scrollerEnded.findImage(area) != null;
    } catch (IOException e) {
      e.printStackTrace();
    }
    return false;
  }

  private void writeImage(Rectangle area, String filename) {

    _scanner.writeImage(area, filename);

  }

  public void addNewTrains(List<Train> trains, List<Train> newTrains) {
    List<Train> notFound = new ArrayList<>();
    for (Train newTrain : newTrains) {
      boolean found = false;
      for (Train train : trains) {
        Pixel p = _comparator.findImage(train.getScanImage(), newTrain.getScanImage());
        if (p == null && isLocoOnly()) {
          BufferedImage i1 = train.getScanImage();
          BufferedImage i2 = newTrain.getScanImage();
          p = _comparator.findImage(i1.getSubimage(i1.getWidth() - 93, 0, 93, i1.getHeight()),
              i2.getSubimage(i2.getWidth() - 93, 0, 93, i2.getHeight()));

        }
        if (p != null) {
          merge(train, newTrain);
          found = true;
          break;
        }
      }
      if (!found) {
        notFound.add(newTrain);
      }
    }// for newTrains
    trains.addAll(notFound);
  }

  public void mergeTrains(List<Train> trains, List<Train> newTrains) {
    List<Train> notFound = new ArrayList<>();
    List<Train> old = new ArrayList<>();
    for (Train newTrain : newTrains) {
      boolean found = false;
      for (Train train : trains) {
        Pixel p = _comparator.findImage(train.getScanImage(), newTrain.getScanImage());
        if (p == null && isLocoOnly()) {
          BufferedImage i1 = train.getScanImage();
          BufferedImage i2 = newTrain.getScanImage();
          p = _comparator.findImage(i1.getSubimage(i1.getWidth() - 93, 0, 93, i1.getHeight()),
              i2.getSubimage(i2.getWidth() - 93, 0, 93, i2.getHeight()));

        }
        if (p != null) {
          merge(train, newTrain);

          found = true;

          old.add(train);
          trains.remove(train);
          break;
        }
      }
      if (!found) {
        notFound.add(newTrain);
      }
    }// for newTrains

    trains.clear();
    trains.addAll(old);
    trains.addAll(notFound);

  }

  private void merge(Train train, Train newTrain) {
    
    train.mergeWith(newTrain);
//    
//    train.setFullImageFilename(newTrain.getFullImageFileName());
//    train.setFullImage(newTrain.getFullImage());
//
//    if (newTrain.isIdle())
//      train.setScanImageFilename(newTrain.getScanImageFileName());
//    else {
//      train.setContractorsBeenSent(newTrain.getContractorsBeenSent());
//    }
//
//    train.setAdditionalInfoFilename(newTrain.getAdditionalInfoFileName());
//    train.setAdditionalInfo(newTrain.getAdditionalInfo());
//
//    train.setAdditionalInfoShortFilename(newTrain.getAdditionalInfoShortFileName());
//    train.setAdditionalInfoShort(newTrain.getAdditionalInfoShort());
//
//    train.setIdle(true);
//    train.setTimeToSendNext(0l);
  }

  public static void main(String[] args) {
    List<String> trains = new ArrayList<>();

    trains.add("1");
    trains.add("2");
    trains.add("3");
    trains.add("4");
    trains.add("5");
    String[] s = new String[] { "3", "1", "5" };
    for (int i = 0; i < 3; i++) {
      for (String t : trains) {
        System.err.println(t);
        if (t.equals(s[i])) {
          trains.remove(t);
          System.err.println("removing " + s[i]);
          break;
        }
      }
      System.err.println();
    }
  }

  public boolean isLocoOnly() {
    return _locoOnly;
  }

  public void setLocoOnly(boolean locoOnly) {
    _locoOnly = locoOnly;
  }

}
