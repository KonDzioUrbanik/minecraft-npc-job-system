package org.example.pans.aINPC.jobsystem;

import org.bukkit.configuration.ConfigurationSection;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JobType {
    private static final List<String> jobTypes = new ArrayList<>();
    private static final Map<String, Map<Integer, Integer>> jobLevels = new HashMap<>();

    public static List<String> getJobTypes() {
        return jobTypes;
    }

    public static boolean isValidJob(String name) {
        return jobTypes.contains(name.toUpperCase());
    }

    public static void loadFromConfig(List<String> configList, ConfigurationSection levelsSection) {
        jobTypes.clear();
        for (String job : configList) {
            jobTypes.add(job.toUpperCase());
            if (levelsSection.isConfigurationSection(job)) {
                Map<Integer, Integer> levelMap = new HashMap<>();
                for (String key : levelsSection.getConfigurationSection(job).getKeys(false)) {
                    levelMap.put(Integer.parseInt(key), levelsSection.getInt(job + "." + key));
                }
                jobLevels.put(job.toUpperCase(), levelMap);
            }
        }
    }

    public static Map<Integer, Integer> getLevelMap(String job) {
        return jobLevels.getOrDefault(job.toUpperCase(), new HashMap<>());
    }
}
