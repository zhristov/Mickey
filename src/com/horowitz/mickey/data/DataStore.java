package com.horowitz.mickey.data;

import java.io.File;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

public class DataStore {

  private Gson _gson = new GsonBuilder().setPrettyPrinting().create();

  public Contractor[] readContractors() throws IOException {
    String json = FileUtils.readFileToString(new File("data/contractors.json"));

    Contractor[] contractors = _gson.fromJson(json, Contractor[].class);

    return contractors;
  }

  public void writeContractors(Contractor[] contractors) throws IOException {

    String json = _gson.toJson(contractors);

    FileUtils.writeStringToFile(new File("data/contractors.json"), json);
  }

  public Mission[] readMissions(String contractor) throws IOException {
    String json = FileUtils.readFileToString(new File("data/" + contractor + "_missions.json"));

    Mission[] missions = _gson.fromJson(json, Mission[].class);

    return missions;
  }

  public void writeMissions(String contractor, Mission[] missions) throws IOException {

    String json = _gson.toJson(missions);

    FileUtils.writeStringToFile(new File("data/" + contractor + "_missions.json"), json);
  }

  public Mission[] readCurrentMissions() throws IOException {
    File file = new File("data/current_missions.json");
    if (file.exists()) {
      String json = FileUtils.readFileToString(file);

      Mission[] missions = _gson.fromJson(json, Mission[].class);

      return missions;
    } else {
      return null;
    }
  }

  public void writeCurrentMissions(Mission[] currentMissions) throws IOException {
    String json = _gson.toJson(currentMissions);

    FileUtils.writeStringToFile(new File("data/current_missions.json"), json);
  }
}
