package net.xtrafrancyz.degustator.module.synchronizer;

import discord4j.common.util.Snowflake;

/**
 * @author xtrafrancyz
 */
public class GetDiscordIdResponse {
    public Snowflake id;
    public String nick;
    
    public GetDiscordIdResponse(Snowflake id, String nick) {
        this.id = id;
        this.nick = nick;
    }
}
