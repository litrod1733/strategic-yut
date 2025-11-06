package server.game;

import java.util.*;

public class Tokens {
	private final Deque<Integer> stack = new ArrayDeque<>();

	public void add(int steps) {
		stack.addLast(steps);
	}

	public void addOutcome(Throwing.Outcome o) {
		add(o.steps);
	}

	public boolean isEmpty() {
		return stack.isEmpty();
	}

	public int size() {
		return stack.size();
	}

	public Integer popFront() {
		return stack.pollFirst();
	}

	public void addFront(int steps) {
		stack.addFirst(steps);
	}
}
