import java.io.PrintStream;
import java.nio.charset.Charset;
import java.util.Scanner;

import javax.swing.JTextField;

public class Main
{

	public static void main(String args[])
	{

		MyFrame f = new MyFrame();
		Thread t=new Thread(f);
		t.start();
	}
}
