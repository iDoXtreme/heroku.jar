package com.heroku.api.command.key;

import com.heroku.api.Heroku;
import com.heroku.api.command.Command;
import com.heroku.api.command.CommandConfig;
import com.heroku.api.command.response.Unit;
import com.heroku.api.exception.RequestFailedException;
import com.heroku.api.http.Http;

import java.util.Map;

/**
 * TODO: Javadoc
 *
 * @author James Ward
 */
public class KeyAdd implements Command<Unit> {

    // post("/user/keys", key, { 'Content-Type' => 'text/ssh-authkey' }).to_s

    private final CommandConfig config;

    public KeyAdd(String sshkey) {
        this.config = new CommandConfig().with(Heroku.RequestKey.sshkey, sshkey);
    }

    @Override
    public Http.Method getHttpMethod() {
        return Http.Method.POST;
    }

    @Override
    public String getEndpoint() {
        return Heroku.Resource.Keys.value;
    }

    @Override
    public boolean hasBody() {
        return true;
    }

    @Override
    public String getBody() {
        return config.get(Heroku.RequestKey.sshkey);
    }

    @Override
    public Http.Accept getResponseType() {
        return Http.Accept.JSON;
    }

    @Override
    public Map<String, String> getHeaders() {
        return Http.Header.Util.setHeaders(Http.ContentType.SSH_AUTHKEY);
    }

    public Unit getResponse(byte[] in, int code) {
        if (code == Http.Status.OK.statusCode)
            return Unit.unit;
        else
            throw new RequestFailedException("KeysAdd failed", code, in);
    }

}