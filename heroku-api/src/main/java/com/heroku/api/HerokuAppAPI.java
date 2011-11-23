package com.heroku.api;


import com.heroku.api.command.app.AppCreate;
import com.heroku.api.command.app.AppDestroy;
import com.heroku.api.command.app.AppInfo;
import com.heroku.api.command.config.ConfigAdd;
import com.heroku.api.command.config.ConfigList;
import com.heroku.api.command.config.ConfigRemove;
import com.heroku.api.command.log.Log;
import com.heroku.api.command.response.JsonMapResponse;
import com.heroku.api.command.sharing.CollabList;
import com.heroku.api.command.sharing.SharingAdd;
import com.heroku.api.command.sharing.SharingRemove;
import com.heroku.api.command.sharing.SharingTransfer;
import com.heroku.api.connection.Connection;

import java.util.List;
import java.util.Map;

public class HerokuAppAPI {

    final Connection<?> connection;
    final String appName;

    public HerokuAppAPI(Connection<?> connection, String name) {
        this.connection = connection;
        this.appName = name;
    }

    public Map<String, String> create(Heroku.Stack stack) {
        return connection.executeCommand(new AppCreate(stack).withName(appName)).getData();
    }

    public HerokuAppAPI createAnd(Heroku.Stack stack) {
        create(stack);
        return this;
    }

    public void destroy() {
        connection.executeCommand(new AppDestroy(appName));
    }

    public Map<String, String> info() {
        return connection.executeCommand(new AppInfo(appName)).getData();
    }

    public List<Map<String, String>> listCollaborators() {
        return connection.executeCommand(new CollabList(appName)).getData();
    }

    public HerokuAppAPI addCollaborator(String collaborator) {
        connection.executeCommand(new SharingAdd(appName, collaborator));
        return this;
    }

    public HerokuAppAPI removeCollaborator(String collaborator) {
        connection.executeCommand(new SharingRemove(appName, collaborator));
        return this;
    }

    public void addConfig(String config) {
        connection.executeCommand(new ConfigAdd(appName, config));
    }

    public Map<String, String> listConfig() {
        return connection.executeCommand(new ConfigList(appName)).getData();
    }

    public Map<String, String> removeConfig(String configVarName) {
        return connection.executeCommand(new ConfigRemove(appName, configVarName)).getData();
    }

    public void transferApp(String to) {
        connection.executeCommand(new SharingTransfer(appName, to));
    }

    public String getLogChunk() {
        return connection.executeCommand(connection.executeCommand(new Log(appName)).getNextCommand()).getText();
    }

    public HerokuAPI api() {
        return new HerokuAPI(connection);
    }

    public String getAppName() {
        return appName;
    }

    public Connection getConnection() {
        return connection;
    }
}