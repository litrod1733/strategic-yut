package common.dto;

public class Message {
	public final String type; // 예: "CHOOSE", "MOVE", "CHAT_T", "UPDATE"
	public final Object payload;

	public Message(String type, Object payload) {
		this.type = type;
		this.payload = payload;
	}
}
