/*
 * Copyright 2013 Netflix, Inc.
 *
 *      Licensed under the Apache License, Version 2.0 (the "License");
 *      you may not use this file except in compliance with the License.
 *      You may obtain a copy of the License at
 *
 *          http://www.apache.org/licenses/LICENSE-2.0
 *
 *      Unless required by applicable law or agreed to in writing, software
 *      distributed under the License is distributed on an "AS IS" BASIS,
 *      WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *      See the License for the specific language governing permissions and
 *      limitations under the License.
 */
package com.netflix.zuul.scriptManager;

import com.netflix.config.DynamicBooleanProperty;
import com.netflix.config.DynamicPropertyFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * @author Mikey Cohen
 * Date: 6/15/12
 * Time: 3:44 PM
 */
public class ZuulFilterPoller {

    Map<String, FilterInfo> runningFilters = new HashMap<String, FilterInfo>();
    ZuulFilterDAO dao;

    DynamicBooleanProperty active = DynamicPropertyFactory.getInstance().getBooleanProperty("zuul.use.active.filters", true);
    DynamicBooleanProperty canary = DynamicPropertyFactory.getInstance().getBooleanProperty("zuul.use.canary.filters", false);


    private static ZuulFilterPoller INSTANCE;


    /**
     * Starts the check against the ZuulFilter data store for changed or new filters.
     * @param dao
     */
    public static void start(ZuulFilterDAO dao) {

        INSTANCE = new ZuulFilterPoller(dao);

    }



    public ZuulFilterPoller(ZuulFilterDAO dao) {
        this.dao = dao;
        checkerThread.start();

    }

    public static ZuulFilterPoller getInstance() {
        return INSTANCE;
    }


    boolean running = true;
    private long INTERVAL = 30000; //30 seconds
    Thread checkerThread = new Thread("ZuulFilterPoller") {
        public void run() {
            while (running) {
                try {

                    if (canary.get()) {
                        HashMap<String, FilterInfo> setFilters = new HashMap<String, FilterInfo>();

                        List<FilterInfo> activeScripts = dao.getAllActiveScripts();
                        if (activeScripts != null) {
                            for (FilterInfo newFilter : activeScripts) {
                                setFilters.put(newFilter.getFilterID(), newFilter);
                            }
                        }

                        List<FilterInfo> canaryScripts = dao.getAllCanaryScripts();
                        if (canaryScripts != null) {
                            for (FilterInfo newFilter : canaryScripts) {
                                setFilters.put(newFilter.getFilterID(), newFilter);
                            }
                        }
                        for (FilterInfo next : setFilters.values()) {
                            doFilterCheck(next);
                        }
                    }
                    else if (active.get()) {
                        List<FilterInfo> newFilters = dao.getAllActiveScripts();
                        if (newFilters == null) continue;
                        for (FilterInfo newFilter : newFilters) {
                            doFilterCheck(newFilter);
                        }

                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                }

                try {
                    sleep(INTERVAL);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    running = false;
                }
            }
        }
    };

    private void doFilterCheck(FilterInfo newFilter) throws IOException {
        FilterInfo existingFilter = runningFilters.get(newFilter.getFilterID());
        if (existingFilter == null || !existingFilter.equals(newFilter)) {
            System.out.println("adding filter to disk" + newFilter.toString());
            writeFilterToDisk(newFilter);
            runningFilters.put(newFilter.getFilterID(), newFilter);
        }
    }

    private void writeFilterToDisk(FilterInfo newFilter) throws IOException {

        String path = DynamicPropertyFactory.getInstance().getStringProperty("zuul.filter.pre.path", null).get();
        if (newFilter.getFilterType().equals("post")) {
            path = DynamicPropertyFactory.getInstance().getStringProperty("zuul.filter.post.path", null).get();
        }
        if (newFilter.getFilterType().equals("route")) {
            path = DynamicPropertyFactory.getInstance().getStringProperty("zuul.filter.routing.path", null).get();
        }

        File f = new File(path, newFilter.getFilterName() + ".groovy");
        FileWriter file = new FileWriter(f);
        BufferedWriter out = new BufferedWriter(file);
        out.write(newFilter.getFilterCode());
        out.close();
        file.close();
        System.out.println("filter written " + f.getPath());
    }




}