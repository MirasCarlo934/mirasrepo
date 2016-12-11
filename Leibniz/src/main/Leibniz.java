package main;

import org.apache.log4j.Logger;

import input.InputReader;

public class Leibniz {
	private static final Logger log = Logger.getLogger("MAIN.Leibniz");
	private static final InputReader ir = new InputReader();

	public Leibniz() {
		
	}

	public static void main(String[] args) {
		log.info(ir.read());
	}
}
