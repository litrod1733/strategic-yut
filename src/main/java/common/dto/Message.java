package common.dto;

import java.util.Objects;

public class Message {
	public String type; // 예: "CHOOSE", "MOVE", "CHAT_T", "UPDATE"
	public Object payload;

	public Message(String type, Objects payload) {
		this.type = type;
		this.payload = payload;
	}
}
