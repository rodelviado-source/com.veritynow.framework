package com.veritynow.core.context;

import java.util.Stack;


public class ContextStack implements ContextStorage {
	private final ThreadLocal<Stack<ContextSnapshot>> stack = ThreadLocal.withInitial(() -> new Stack<>());

	    public void push(ContextSnapshot item) {
	        stack.get().push(item);
	    }

	    public ContextSnapshot pop() {
	        return stack.get().pop();
	    }
	
	    public ContextSnapshot peek() {
	    	if (stack.get().isEmpty())
	    		return null;
	    	return stack.get().peek();
	    }

		@Override
		public ContextSnapshot currentOrNull() {
			return peek();
		}

		@Override
		public void bind(ContextSnapshot snapshot) {
			push(snapshot);			
		}

		@Override
		public void clear() {
			pop();
		}

		@Override
		public int size() {
			return stack.get().size();
		}
		
		

}
