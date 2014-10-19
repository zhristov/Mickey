package com.horowitz.mickey;

import java.awt.Color;
import java.awt.Rectangle;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;

import com.horowitz.mickey.data.DataStore;
import com.horowitz.mickey.data.Mission;
import com.horowitz.mickey.data.Objective;
import com.horowitz.mickey.ocr.OCR;
import com.horowitz.mickey.ocr.OCR2;

public class MissionScanner {

  private OCR                          _ocr;

  private static final ImageComparator COMPARATOR  = new SimilarityImageComparator(0.04, 20);

  private List<ImageDataExt>           _objectives = new ArrayList<>();

  public MissionScanner() {
    super();
    _ocr = new OCR("masks.txt");// TODO
    try {
      // Steam
      _objectives.add(new ImageDataExt(new ImageData("ocr/steamTrains.bmp", null, COMPARATOR, 0, 0), "Steam", new Rectangle(-13, 53, 100, 22)));
      // Diesel
      _objectives.add(new ImageDataExt(new ImageData("ocr/dieselTrains.bmp", null, COMPARATOR, 0, 0), "Diesel", new Rectangle(-13, 53, 100, 22)));

      // Electric
      // TODO

      // Maglev
      // TODO
    } catch (IOException e) {
    }

    // Gold
    addObjectives("Gold");
    addObjectives("Wood");
    addObjectives("Nails");
    addObjectives("Bricks");
    addObjectives("Glass");
    addObjectives("Fuel");
    addObjectives("Steel");
    addObjectives("Gravel");
    addObjectives("U-235");
    addObjectives("Cement");
    addObjectives("Rubber");
    addObjectives("Carbon");
    addObjectives("Titanium");
    addObjectives("Marble");
    addObjectives("Wires");
    addObjectives("Plastics");
    addObjectives("Silicon");

  }

  private void addObjectives(String name) {
    try {
      _objectives.add(new ImageDataExt(new ImageData("ocr/" + name + "10.bmp", null, COMPARATOR, 0, 0), name, new Rectangle(40, -15, 165, 55)));
    } catch (Exception e) {
      //System.err.println(e.getMessage());
    }

    try {
      _objectives.add(new ImageDataExt(new ImageData("ocr/" + name + "8.bmp", null, COMPARATOR, 0, 0), name, new Rectangle(20, -10, 140, 22)));
    } catch (Exception e) {
      //System.err.println(e.getMessage());
    }
  }

  public int[] scanMissionNumbers(BufferedImage contractorImage) {
    BufferedImage subimage = contractorImage.getSubimage(376, 388, 71, 21);
    OCR2 ocr = OCR2.createBlue();

    String res = ocr.scanImage(subimage);
    System.err.println(res);
    if (res.indexOf("/") > 0) {
      String[] ss = res.split("/");
      int[] numbers = new int[2];
      try {
        numbers[0] = Integer.parseInt(ss[0]);
        numbers[1] = Integer.parseInt(ss[1]);
        return numbers;
      } catch (NumberFormatException e) {
        // TODO handle OCR errors better
        e.printStackTrace();
      }
    }
    return null;
  }

  private void writeImage(BufferedImage image, int n) {
    if (System.getenv("DEBUG") != null)
      try {
        ImageIO.write(image, "PNG", new File("subimage" + n + ".png"));
      } catch (IOException e) {
        e.printStackTrace();
      }
  }

  public void scanCurrentMission(BufferedImage contractorImage, Mission mission) {
    // this is the Objectives area without the description part
    writeImage(contractorImage, 2001);
    BufferedImage objArea = contractorImage.getSubimage(456, 60 + 93, 288, 262);
    writeImage(objArea, 2002);
    BufferedImage subimage = objArea.getSubimage(0, 0, objArea.getWidth(), objArea.getHeight());
    writeImage(subimage, 2003);

    for (Objective o : mission.getObjectives()) {
      String what = o.getMaterial();

      ImageDataExt theIDE = null;
      for (ImageDataExt ide : _objectives) {
        if (ide._name.equals(what)) {
          theIDE = ide;
          break;
        }
      }
      if (theIDE != null) {
        OCR2 ocrRed = OCR2.createRed();
        OCR2 ocrGray = OCR2.createGray();
        Pixel p = theIDE._imageData.findImage(subimage);
        if (p != null) {
          // found the resource. Now let's scan the numbers
          BufferedImage textImage = subimage.getSubimage(p.x + theIDE._correspondingArea.x, p.y + theIDE._correspondingArea.y,

          subimage.getWidth() - p.x - theIDE._correspondingArea.x, theIDE._correspondingArea.height);
          writeImage(textImage, 203);
          String resRed = ocrRed.scanImage(textImage);
          String resGray = ocrGray.scanImage(textImage);

          String res = resRed != null && resRed.length() > 0 ? resRed + resGray : resGray;
          if (res != null) {
            // set values to the existing objective
            String[] ss = res.split("/");
            o.setCurrentAmount(Integer.parseInt(ss[0]));
            o.setNeededAmount(Integer.parseInt(ss[1]));
          }
        }
      }// if

    }// for
  }

  public static void main(String[] args) {
//    testContractor("bobbymaterials.bmp", "Bobby");
//    testContractor("status 02A mahatma.bmp", "Mahatma");
//    testContractor("status 03A george.bmp", "George");
//    testContractor("status 04A otto.bmp", "Otto");
//    testContractor("status 05A sam.bmp", "Sam");
//    testContractor("status 06A alan.bmp", "Alan");
//    testContractor("status 07A wolfgang.bmp", "Wolfgang");
//    testContractor("status 08A mizuki.bmp", "Mizuki");
//    testContractor("status 09A lucy.bmp", "Lucy");
//    testContractor("status 10A giovanni.bmp", "Giovanni");
//    testContractor("1/status 01A bobby.bmp", "Bobby");
//    testContractor("1/status 02A mahatma.bmp", "Mahatma");
//    testContractor("1/status 03A george.bmp", "George");
//    testContractor("1/status 04A otto.bmp", "Otto");
//    testContractor("1/status 05A sam.bmp", "Sam");
//    testContractor("1/status 06A alan.bmp", "Alan");
//    testContractor("1/status 07A wolfgang.bmp", "Wolfgang");
//    testContractor("1/status 08A mizuki.bmp", "Mizuki");
//    testContractor("1/status 09A lucy.bmp", "Lucy");
//    testContractor("1/status 10A giovanni.bmp", "Giovanni");
    testContractor("2/status 01A bobby.bmp", "Bobby");
    testContractor("2/status 02A mahatma.bmp", "Mahatma");
    testContractor("2/status 03A george.bmp", "George");
    testContractor("2/status 04A otto.bmp", "Otto");
    testContractor("2/status 05A sam.bmp", "Sam");
    testContractor("2/status 06A alan.bmp", "Alan");
    testContractor("2/status 07A wolfgang.bmp", "Wolfgang");
    testContractor("2/status 08A mizuki.bmp", "Mizuki");
    testContractor("2/status 09A lucy.bmp", "Lucy");
    testContractor("2/status 10A giovanni.bmp", "Giovanni");

  }

  private static void testContractor(String image1, String cont1) {
    System.out.println("Testing " + cont1);
    try {
      BufferedImage image = ImageIO.read(ImageManager.getImageURL(image1));
      Settings settings = new Settings();
      settings.loadSettings();
      ScreenScanner sscanner = new ScreenScanner(settings);

      Pixel p = sscanner.getContracts().findImage(image);
      if (p != null) {
        image = image.getSubimage(p.x - 38, p.y - 28, 779, 584);
        MissionScanner scanner = new MissionScanner();
        int[] numbers = scanner.scanMissionNumbers(image);
        System.err.println(numbers);
        if (numbers != null) {
          int number = numbers[0];
          Mission[] readMissions = new DataStore().readMissions(cont1);
          for (Mission m : readMissions) {
            if (m.getNumber() == number) {
              scanner.scanCurrentMission(image, m);
              System.out.println(cont1 + " " + m);
              break;
            }
          }
        }
      }

    } catch (IOException e) {
      // TODO Auto-generated catch block
      e.printStackTrace();
    }
  }

  private static class ImageDataExt {
    ImageData _imageData;
    String    _name;
    Rectangle _correspondingArea;

    public ImageDataExt(ImageData imageData, String name, Rectangle correspondingArea) {
      super();
      _imageData = imageData;
      _name = name;
      _correspondingArea = correspondingArea;
    }

  }

  public Mission scanCurrentMission(BufferedImage contractorImage) {
    // TODO Auto-generated method stub
    return null;
  }

}
