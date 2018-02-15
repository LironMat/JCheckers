import java.awt.BorderLayout;
import java.awt.event.KeyEvent;
import java.awt.event.KeyListener;
import java.nio.charset.Charset;

import javax.swing.JFrame;
import javax.swing.JTextArea;

public class MyFrame extends JFrame implements KeyListener, Runnable
{
	public JTextArea _board, _messages;

	int curNum = -1;

	int occupied = 0xFFF00FFF; // 0 empty, 1 occupied
	int white = 0xFFF00000; // 0 black or empty, 1 white
	int king = 0x00000000; // 0 not king, 1 king
	
	int grid = 1;

	public MyFrame()
	{
		this.setFocusable(true);
		setLayout(new BorderLayout());
		_board = new JTextArea();
		_board.setEditable(false);
		add(_board, BorderLayout.CENTER);
		_messages = new JTextArea();
		_messages.setEditable(false);
		add(_messages, BorderLayout.SOUTH);
		addKeyListener(this);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		setExtendedState(JFrame.MAXIMIZED_BOTH);
		setUndecorated(true);
		setVisible(true);
	}

	public void game()
	{
		int player = 1;
		String stats = "";

		while (countOn(white) != 0 && (countOn(occupied) - countOn(white)) != 0)
		{
			stats = String.format(
					"player: %d\nint occupied= 0x%x, white= 0x%x, king= 0x%x;\npieces remain %d, white %d black %d \neating options count: %d \n",
					player, occupied, white, king, countOn(occupied), countOn(white),
					countOn(occupied) - countOn(white), eatingOptions(player, 0));
			printBoard(player, stats);
			turn(player, stats);
			player = (player == 1) ? 0 : 1;
		}
		printBoard(player, stats);
		if (countOn(white) == 0)
			printLnText(_messages, "black won");
		else printLnText(_messages, "white won");
	}

	public void turn(int player, String stats)
	{
		int fromX, fromY, toX, toY, fromLoc, toLoc, ok = 0, eatX, eatY, eatLoc, okChain;
		while (ok == 0)
		{
			printLnText(_messages, "select the row of the piece you want to move");
			waitForPress();
			fromY = curNum;
			curNum = -1;
			printLnText(_messages, "select the col of the piece you want to move");
			waitForPress();
			fromX = curNum;
			curNum = -1;
			printLnText(_messages, "select the row of where you want to move");
			waitForPress();
			toY = curNum;
			curNum = -1;
			printLnText(_messages, "select the col of where you want to move");
			waitForPress();
			toX = curNum;
			curNum = -1;

			fromLoc = getLoc(fromX, fromY);
			toLoc = getLoc(toX, toY);
			eatX = (toX + fromX) / 2;
			eatY = (toY + fromY) / 2;
			eatLoc = getLoc(eatX, eatY);
			printBoard(player, stats);
			if (checkInLegalBoard(fromX, fromY) != 0)
			{
				if (isOccupied(fromLoc) != 0)
				{
					if (isWhite(fromLoc) == player)
					{
						if (checkInLegalBoard(toX, toY) != 0)
						{
							if (isOccupied(toLoc) == 0)
							{
								if (eatingOptions(player, 0) == 0 && (toX - fromX == -1 || toX - fromX == 1)
										&& (toY - fromY == (1 - 2 * player)
												|| (Math.abs(toY - fromY) == 1 && isKing(fromLoc) != 0)))
								{
									move(fromLoc, toX, toY, toLoc, player);
									ok = 1;
								}
								else
								{
									if (checkCanEat(fromX, toX, fromY, toY, fromLoc, eatLoc, player, 0) != 0)
									{
										move(fromLoc, toX, toY, toLoc, player);
										clear(eatLoc);
										ok = 1;
										while (optionsFromPlace(toX, toY, player, 1) != 0)
										{
											okChain = 0;
											fromX = toX;
											fromY = toY;
											fromLoc = getLoc(fromX, fromY);
											stats = String.format(
													"player: %d\noccupied %x white %x king %x\npieces remain %d, white %d black %d \neating options count: %d \n",
													player, occupied, white, king, countOn(occupied), countOn(white),
													countOn(occupied) - countOn(white),
													optionsFromPlace(fromX, fromY, player, 1));
											while (okChain == 0)
											{
												printBoard(player, stats);
												printLnText(_messages, "select the row of continuing the chain");
												waitForPress();
												toY = curNum;
												curNum = -1;
												printLnText(_messages, "select the col of continuing the chain");
												waitForPress();
												toX = curNum;
												curNum = -1;

												toLoc = getLoc(toX, toY);
												eatX = (toX + fromX) / 2;
												eatY = (toY + fromY) / 2;
												eatLoc = getLoc(eatX, eatY);
												if (checkInLegalBoard(toX, toY) != 0)
												{
													if (isOccupied(toLoc) == 0)
													{
														if (checkCanEat(fromX, toX, fromY, toY, fromLoc, eatLoc, player,
																1) != 0)
														{
															move(fromLoc, toX, toY, toLoc, player);
															clear(eatLoc);
															okChain = 1;
														}
														else printLnText(_messages, "must continue the chain");
													}
													else printLnText(_messages, "to: place is occupied");
												}
												else printLnText(_messages, "to: not on legal board");
											}
										}
									}
									else printLnText(_messages, "must eat or cant go idk");
								}
							}
							else printLnText(_messages, "to: piece here");
						}
						else printLnText(_messages, "to: not within the board");
					}
					else printLnText(_messages, "from: not your piece");
				}
				else printLnText(_messages, "from: no piece here");
			}
			else printLnText(_messages, "from: not within the board");
		}

	}

	int isOccupied(int loc)
	{
		return ((occupied) >> loc) & 1;
	}

	int isWhite(int loc)
	{
		return ((white) >> loc) & 1;
	}

	int isKing(int loc)
	{
		return ((king) >> loc) & 1;
	}

	int checkCanEat(int fromX, int toX, int fromY, int toY, int fromLoc, int eatLoc, int player, int chain)
	{
		return ((toX - fromX == -2 || toX - fromX == 2)
				&& (toY - fromY == 2 * (1 - 2 * player)
						|| (Math.abs(toY - fromY) == 2 && (chain != 0 || isKing(fromLoc) != 0)))
				&& isOccupied(eatLoc) != 0 && (isWhite(eatLoc) != player && isOccupied(getLoc(toX, toY)) == 0)) ? 1 : 0;
	}

	int checkInLegalBoard(int x, int y)
	{
		return (x >= 0 && y >= 0 && x < 8 && y < 8
				&& (((x % 2) != 0 && (y % 2) != 0) || ((x % 2) == 0 && (y % 2) == 0))) ? 1 : 0;
	}

	void konigMaker(int toX, int toY, int toLoc, int player)
	{
		if (toY == 0 && player != 0 || toY == 7 && player == 0)
			king = king | (1 << toLoc);
	}

	void move(int fromLoc, int toX, int toY, int toLoc, int player)
	{
		king = king | (isKing(fromLoc) << toLoc);
		occupied = occupied | (1 << toLoc);
		white = white | (player << toLoc);
		clear(fromLoc);
		konigMaker(toX, toY, toLoc, player);
	}

	void clear(int loc)
	{
		occupied = occupied & ~(1 << loc);
		white = white & ~(1 << loc);
		king = king & ~(1 << loc);
	}

	int countOn(int num)
	{
		int count = (num >> 31) & 1;
		num = num & ~(1 << 31);
		while (num != 0)
		{
			count += num & 1;
			num = num >> 1;
		}
		return count;
	}

	int eatingOptions(int player, int chain)
	{
		int i, x, y, count = 0;
		for (i = 0, count = 0; i < 32; i++)
		{
			if (isOccupied(i) != 0 && (isWhite(i) == player))
			{
				y = i / 4;
				x = 2 * (i % 4) + y % 2;
				count += optionsFromPlace(x, y, player, chain);
			}
		}
		return count;
	}

	int optionsFromPlace(int x, int y, int player, int chain)
	{
		int count = 0;

		if (checkInLegalBoard(x - 2, y - 2) != 0
				&& checkCanEat(x, x - 2, y, y - 2, getLoc(x, y), getLoc(x - 1, y - 1), player, chain) != 0)
			count++;

		if (checkInLegalBoard(x + 2, y - 2) != 0
				&& checkCanEat(x, x + 2, y, y - 2, getLoc(x, y), getLoc(x + 1, y - 1), player, chain) != 0)
			count++;

		if (checkInLegalBoard(x - 2, y + 2) != 0
				&& checkCanEat(x, x - 2, y, y + 2, getLoc(x, y), getLoc(x - 1, y + 1), player, chain) != 0)
			count++;

		if (checkInLegalBoard(x + 2, y + 2) != 0
				&& checkCanEat(x, x + 2, y, y + 2, getLoc(x, y), getLoc(x + 1, y + 1), player, chain) != 0)
			count++;
		return count;
	}

	int getLoc(int x, int y)
	{
		return (4 * y + x / 2);
	}

	public void printChar(String charToPrint)
	{
		try
		{
			Charset utf8 = Charset.forName("UTF-8");
			byte[] bytes = charToPrint.getBytes("UTF-8");
			String message = new String(bytes, utf8.name());
			printText(_board, message);
			// PrintStream printStream = new PrintStream(, true, utf8.name());
			// printStream.print(message); // should print your character
		}
		catch (Exception e)
		{

		}
	}

	public void printText(JTextArea field, String msg)
	{
		field.setText(field.getText() + msg);
	}

	public void printLnText(JTextArea field, String msg)
	{
		field.setText(field.getText() + String.format("\n") + msg);
	}

	public void printBoard(int player, String stats)
	{
		int type, i;
		_messages.setText("");
		_board.setText("");
		printLnText(_messages, stats);
		if (grid != 0)
			for (i = 0, printText(_board, "   "); i < 8; i++)
			{
				printText(_board, String.valueOf(i) + "   ");
				if (i == 7)
					printLnText(_board, "");
			}

		for (i = 0; i < 32; i++)
		{
			type = (isOccupied(i)) + (isWhite(i) << 1) + (isKing(i) << 2);
			if (grid != 0 && (i % 4) == 0)
				printText(_board, String.valueOf(i / 4));

			if (((i / 4) % 2) != 0)
				printChar("■  ");
			switch (type)
			{
				case 0:
					printText(_board, "    ");
					break;
				case 1:
					printChar("☻ ");
					break;
				case 3:
					printChar("☺ ");
					break;
				case 5:
					printChar("♚ ");
					break;
				case 7:
					printChar("♔ ");
					break;
			}
			if (((i / 4) % 2) == 0)
				printChar("■  ");
			if (grid != 0 && (i % 4) == 3)
				printText(_board, String.valueOf(i / 4));
			if ((i % 4) == 3)
				printLnText(_board, "");
		}
		if (grid != 0)
			for (i = 0, printText(_board, "   "); i < 8; i++)
			{
				printText(_board, String.valueOf(i) + "   ");
				if (i == 7)
					printLnText(_board, "");
			}
	}

	public synchronized void waitForPress()
	{
		while (curNum > 7 || curNum < 0)
		{
			try
			{
				wait();

			}
			catch (InterruptedException e)
			{
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}
	}

	@Override
	public synchronized void keyPressed(KeyEvent arg0)
	{
		// TODO Auto-generated method stub
		curNum = arg0.getKeyChar() - '0';
		notifyAll();
	}

	@Override
	public void keyReleased(KeyEvent e)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void keyTyped(KeyEvent e)
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void run()
	{
		// TODO Auto-generated method stub
		game();
	}

}
