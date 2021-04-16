import javax.swing.*;
import javax.swing.SwingUtilities;
import java.awt.*;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.Socket;

public class SahPainter extends JPanel implements MouseListener, MouseMotionListener
{
	private char[][] board;
	private String player;

	private int rowHeight, colWidth;
	private Font bigFont, mediumFont, smallFont;
	private int boardOffsetX, boardOffsetY;
	private int startX, deltaX, startY, deltaY;
	private int mouseX, mouseY, mouseOffsetX, mouseOffsetY;
	private int offsetRow, offsetCol;
	private static int selectedRow, selectedCol, illegalRow, illegalCol;
	private static int timerIllegal;
	private Color playingNowColor, opponentPlayingColor, illegalMoveColor, darkFieldColor;
	private int menu;
	private int menuSelectionBox; // 1 - VS Bot, 2 - VS Player, 3 - LAN Game

	private static int turn, gameState, checkedRow, checkedCol;

	private boolean LANSetup, LANInitFields, connected;
	private int OKButtonX, OKButtonY, OKButtonWidth, OKButtonHeight;
	private JTextField nameTF, ipTF, portTF;

	private String name, opponentsName, ip;
	private int port;
	private Socket socket;
	private ServerSocket serverSocket;
	private DataOutputStream dos;
	private DataInputStream dis;
	private static boolean sentRestart, opponentRestarted;
	private int myWins, opponentWins;
	private static boolean addedMyWins, addedOpponentWins;


	public SahPainter(char[][] board, String player)
	{
		menu = 0;
		menuSelectionBox = 0;
		rowHeight = (Sah.BOARD_SIZE - 36) / 8;
		colWidth = (Sah.BOARD_SIZE - 16) / 8;

		bigFont = new Font("Courier", Font.PLAIN, 64);
		mediumFont = new Font("Courier", Font.PLAIN, 32);
		smallFont = new Font("Courier", Font.PLAIN, 24);
		this.board = board;
		this.player = player;

		// Weird offset because of MouseListeners....
		mouseOffsetX = 8;
		mouseOffsetY = 32;

		boardOffsetX = 176;
		boardOffsetY = 100;
		offsetRow = 1;
		offsetCol = 2;

		// Figures drawing offset
		startX = 16;
		deltaX = 98;
		startY = 70;
		deltaY = 95;
		playingNowColor = new Color(0, 255, 0, 30);
		opponentPlayingColor = new Color(10, 10, 10, 70);
		illegalMoveColor = new Color(255, 0, 0, 100);
		darkFieldColor = new Color(0, 0, 0, 1);

		LANSetup = true;
		LANInitFields = true;
		OKButtonX = 560;
		OKButtonY = 590;
		OKButtonWidth = 100;
		OKButtonHeight = 50;

		myWins = opponentWins = 0;

		resetGame();
	}

	public static void resetGame()
	{
		selectedRow = -1;
		selectedCol = -1;
		timerIllegal = 0;
		gameState = 0;
		checkedRow = -1;
		checkedCol = -1;
		sentRestart = false;
		opponentRestarted = false;
		addedMyWins = addedOpponentWins = false;
	}


	@Override
	public void paintComponent(Graphics g)
	{
		super.paintComponent(g);
		g.setFont(bigFont);
		g.setColor(Color.black);

		if(menu == 0) // Display menu
		{
			g.drawString("SAH", 450, 70);
			g.drawString("VS Bot", 100, 200);
			g.drawString("VS Player", 100, 300);
			g.drawString("LAN Game", 100, 400);
			g.setColor(Color.black);

			g.drawString("Quit", 100, 750);

			menuSelectionBox = 0;
			if(mouseY > 145 && mouseY < 215)
			{
				menuSelectionBox = 1;
				g.drawLine(0, 145, Sah.WIDTH, 145);
				g.drawLine(0, 210, Sah.WIDTH, 210);
			}
			else if(mouseY > 245 && mouseY < 315)
			{
				menuSelectionBox = 2;
				g.drawLine(0, 245, Sah.WIDTH, 245);
				g.drawLine(0, 310, Sah.WIDTH, 310);
			}
			else if(mouseY > 345 && mouseY < 415)
			{
				menuSelectionBox = 3;
				g.drawLine(0, 345, Sah.WIDTH, 345);
				g.drawLine(0, 410, Sah.WIDTH, 410);
			}
			else if(mouseY > 695 && mouseY < 760)
			{
				menuSelectionBox = 4;
				g.drawLine(0, 695, Sah.WIDTH, 695);
				g.drawLine(0, 760, Sah.WIDTH, 760);
			}
		}
		else if(menu == 1 || menu == 2) // VS Bot or VS Player
		{
			// Figures
			for(int i = 0; i < 8; i++)
			{
				for(int j = 0; j < 8; j++)
				{
					if(i % 2 == 0 && j % 2 == 1 || i % 2 == 1 && j % 2 == 0)
					{
						g.setColor(darkFieldColor);
					}
					else
					{
						g.setColor(Color.white);
					}
					g.fillRect(j * colWidth + boardOffsetX, i * rowHeight + boardOffsetY, colWidth, rowHeight);
					if(selectedRow == -1 && selectedCol == -1) // User didn't select anything, paint all their figures
					{
						for(char f : turn == 0 ? Sah.blackFigures : Sah.whiteFigures)
						{
							if(board[i][j] == f)
							{
								g.setColor(playingNowColor);
								g.fillRect(j * colWidth + boardOffsetX, i * rowHeight + boardOffsetY, colWidth,
										rowHeight);
								break;
							}
						}
					}
					g.setColor(Color.black);
					g.drawString(board[i][j] + "", startX + j * deltaX + boardOffsetX,
							startY + i * deltaY + boardOffsetY);
				}
			}

			if(selectedRow != -1 && selectedCol != -1) // User selected a figure, draw his options
			{
				g.setColor(playingNowColor);
				for(int i = 0; i < 8; i++)
				{
					for(int j = 0; j < 8; j++)
					{
						if(Sah.checkMove(player, turn, selectedRow, selectedCol, i, j))
						{
							g.fillRect(j * colWidth + boardOffsetX, i * rowHeight + boardOffsetY, colWidth, rowHeight);
						}
					}
				}
			}

			g.setColor(Color.black);
			// Board
			for(int i = 0; i < 9; i++) // Horizontal
			{
				g.drawLine(boardOffsetX, i * rowHeight + boardOffsetY, Sah.BOARD_SIZE + boardOffsetX - 17,
						i * rowHeight + boardOffsetY);
			}
			for(int i = 0; i < 9; i++) // Vertical
			{
				g.drawLine(i * colWidth + boardOffsetX, boardOffsetY, i * colWidth + boardOffsetX,
						Sah.BOARD_SIZE + boardOffsetY - 40);
			}

			Graphics2D g2 = (Graphics2D) g;
			g2.setStroke(new BasicStroke(4));

			int[] mouseRowCol = calculateMouseRowCol();
			if(mouseRowCol[0] != -1 && mouseRowCol[1] != -1)
			{
				drawSelectedField(g, Color.black, mouseRowCol[0], mouseRowCol[1]); // Mouse Hover
			}

			if(selectedRow != -1 && selectedCol != -1) // User selected a field
			{
				drawSelectedField(g, Color.green, selectedRow, selectedCol);
			}

			if(timerIllegal > 0)
			{
				drawIllegalMove(g);
			}

			if(gameState != 0)
			{
				g.drawString("ZMAGAL JE " + (gameState == 1 ? "CRNI" : "BELI"), 310, 70);
			}
			if(checkedRow != -1 && checkedCol != -1)
			{
				g.setColor(Color.red);
				g.setFont(smallFont);
				g.drawString("SAH", checkedCol * colWidth + boardOffsetX + 22,
						checkedRow * rowHeight + boardOffsetY + 20);
			}
		}
		else if(menu == 3) // LAN Game
		{
			if(LANSetup)
			{
				g.drawString("LAN GAME", 425, 100);
				g.setFont(smallFont);

				g.drawString("Vnesi ime:", 375, 250);
				g.drawString("Vnesi IP:", 375, 350);
				g.drawString("Vnesi port:", 375, 450);

				g.drawString("OK", 590, 625);
				g.drawRect(OKButtonX, OKButtonY, OKButtonWidth, OKButtonHeight);

				if(LANInitFields)
				{
					addTextFieldComponents();
				}
				LANInitFields = false;
			}
			else // LAN Game
			{
				// Players and stats
				g.setFont(mediumFont);
				g.drawString(name + ": " + myWins, 10, 40);
				g.drawString(opponentsName + ": " + opponentWins, 10, 85);

				// Figures
				g.setFont(bigFont);
				for(int i = 0; i < 8; i++)
				{
					for(int j = 0; j < 8; j++)
					{
						if(i % 2 == 0 && j % 2 == 1 || i % 2 == 1 && j % 2 == 0)
						{
							g.setColor(darkFieldColor);
						}
						else
						{
							g.setColor(Color.white);
						}
						g.fillRect(j * colWidth + boardOffsetX, i * rowHeight + boardOffsetY, colWidth, rowHeight);
						if(selectedRow == -1 && selectedCol == -1) // User didn't select anything, paint all their
						// figures
						{
							for(char f : player.equals("black") && turn == 0 || player.equals("white") && turn == 1 ?
									Sah.blackFigures : Sah.whiteFigures)
							{
								// f holds the figure of the player who's turn it is
								if(board[i][j] == f)
								{
									if(turn == 0) // This one is playing
									{
										g.setColor(playingNowColor);
									}
									else
									{
										g.setColor(opponentPlayingColor);
									}
									g.fillRect(j * colWidth + boardOffsetX, i * rowHeight + boardOffsetY, colWidth,
											rowHeight);
									break;
								}
							}
						}
						g.setColor(Color.black);
						g.drawString(board[i][j] + "", startX + j * deltaX + boardOffsetX,
								startY + i * deltaY + boardOffsetY);
					}
				}

				if(selectedRow != -1 && selectedCol != -1) // User selected a figure, draw his options
				{
					g.setColor(playingNowColor);
					for(int i = 0; i < 8; i++)
					{
						for(int j = 0; j < 8; j++)
						{
							if(Sah.checkMove(player, turn, selectedRow, selectedCol, i, j))
							{
								g.fillRect(j * colWidth + boardOffsetX, i * rowHeight + boardOffsetY, colWidth,
										rowHeight);
							}
						}
					}
				}

				g.setColor(Color.black);
				// Board
				for(int i = 0; i < 9; i++) // Horizontal
				{
					g.drawLine(boardOffsetX, i * rowHeight + boardOffsetY, Sah.BOARD_SIZE + boardOffsetX - 17,
							i * rowHeight + boardOffsetY);
				}
				for(int i = 0; i < 9; i++) // Vertical
				{
					g.drawLine(i * colWidth + boardOffsetX, boardOffsetY, i * colWidth + boardOffsetX,
							Sah.BOARD_SIZE + boardOffsetY - 40);
				}

				Graphics2D g2 = (Graphics2D) g;
				g2.setStroke(new BasicStroke(4));

				int[] mouseRowCol = calculateMouseRowCol();
				if(mouseRowCol[0] != -1 && mouseRowCol[1] != -1)
				{
					drawSelectedField(g, Color.black, mouseRowCol[0], mouseRowCol[1]); // Mouse Hover
				}

				if(selectedRow != -1 && selectedCol != -1) // User selected a field
				{
					drawSelectedField(g, Color.green, selectedRow, selectedCol);
				}

				if(timerIllegal > 0)
				{
					drawIllegalMove(g);
				}

				if(gameState != 0)
				{
					String izpis;
					int xPos;
					if(player.equals("black") && gameState == 1 || player.equals("white") && gameState == 2)
					{
						izpis = "ZMAGA";
						xPos = 360;
						myWins += !addedMyWins ? 1 : 0;
						addedMyWins = true;
					}
					else
					{
						izpis = "ZMAGAL JE " + opponentsName;
						xPos = 310;
						opponentWins += !addedOpponentWins ? 1 : 0;
						addedOpponentWins = true;
					}
					g.drawString(izpis, xPos, 70);
				}
				if(checkedRow != -1 && checkedCol != -1)
				{
					g.setColor(Color.red);
					g.setFont(smallFont);
					g.drawString("SAH", checkedCol * colWidth + boardOffsetX + 22,
							checkedRow * rowHeight + boardOffsetY + 20);
				}
			}
		}
	}

	private int[] calculateMouseRowCol()
	{
		// I have to subtract and add weird offsets because of the board position
		int row = (mouseY - 5) / rowHeight - offsetRow;
		int col = (mouseX + 20) / colWidth - offsetCol;
		if(row < 0 || row > 7 || col < 0 || col > 7)
		{
			return new int[] {-1, -1};
		}
		return new int[] {row, col};
	}

	private void addTextFieldComponents()
	{
		nameTF = new JTextField();
		nameTF.setBounds(495, 220, 230, 45);

		ipTF = new JTextField();
		ipTF.setBounds(495, 320, 230, 45);
//		ipTF.setText("127.0.0.1");

		portTF = new JTextField();
		portTF.setBounds(495, 420, 230, 45);
//		portTF.setText("1234");

		this.add(nameTF);
		this.add(ipTF);
		this.add(portTF);
	}

	private void drawSelectedField(Graphics g, Color color, int row, int col)
	{
		g.setColor(color);
		g.drawRect(col * colWidth + boardOffsetX, row * rowHeight + boardOffsetY, colWidth, rowHeight);
	}

	private void drawIllegalMove(Graphics g)
	{
		g.setColor(illegalMoveColor);
		g.fillRect(illegalCol * colWidth + boardOffsetX, illegalRow * rowHeight + boardOffsetY, colWidth, rowHeight);
		timerIllegal = Math.max(0, timerIllegal - 1);
	}

	public void setIllegalMove(int row, int col)
	{
		illegalRow = row;
		illegalCol = col;
		timerIllegal = 20;
	}


	public void setBoard(char[][] b)
	{
		board = b;
	}

	public void setTurn(int t)
	{
		turn = t;
	}

	public void setWinner(int w)
	{
		gameState = w;
	}

	public void setCheckedKingPosition(int row, int col)
	{
		checkedRow = row;
		checkedCol = col;
	}

	public DataInputStream getDis()
	{
		return dis;
	}

	public boolean getOpponentRestarted()
	{
		return opponentRestarted;
	}

	public boolean getSentRestart()
	{
		return sentRestart;
	}

	public void setOpponentRestarted(boolean restart)
	{
		opponentRestarted = restart;
	}

	private boolean checkLANInput()
	{
		String ip = ipTF.getText();
		String sPort = portTF.getText();

		if(ip.length() == 0 || ip.length() > 15 || sPort.length() == 0)
		{
			return false;
		}

		// Preveri sintakso za IP naslov
		for(int i = 0; i < ip.length(); i++)
		{
			char c = ip.charAt(i);
			if(c < 48 && c != 46 || c > 57) // Ce je manjse od '0' IN ni '.' ALI je vecje od '9'
			{
				return false;
			}
		}

		String[] ipTabela = ip.split("\\.");
		for(String oktet : ipTabela)
		{
			if(Integer.parseInt(oktet) > 255)
			{
				return false;
			}
		}

		// Preveri, ce port vsebuje samo stevilke
		for(int i = 0; i < sPort.length(); i++)
		{
			char c = sPort.charAt(i);
			if(c < 48 || c > 57) // Ce poljuben znak ni stevilka
			{
				return false;
			}
		}

		int iPort = Integer.parseInt(sPort);
		return iPort >= 1024 && iPort <= 65535;
	}

	private boolean connect()
	{
		try
		{
			// Connect to server -> 2. player
			socket = new Socket(ip, port);
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			connected = true;
		}
		catch(IOException e)
		{
			System.out.println("Cannot connect to " + ip + " starting server...");
			return false;
		}
		player = "white";
		turn = 1;
		Sah.setPlayer(player);
		Sah.setTurn(turn);
		Sah.rotateBoard();
		System.out.println("Connected to server!");
		return true;
	}

	private void initializeServer()
	{
		// Naredi server -> to je 1. player
		try
		{
			serverSocket = new ServerSocket(port, 8, InetAddress.getByName(ip));
		}
		catch(Exception e)
		{
			e.printStackTrace();
		}
		player = "black";
		turn = 0;
		Sah.setPlayer(player);
		Sah.setTurn(turn);
	}

	private void listenForServerRequest()
	{
		try
		{
			socket = serverSocket.accept();
			dos = new DataOutputStream(socket.getOutputStream());
			dis = new DataInputStream(socket.getInputStream());
			connected = true;
		}
		catch(IOException e)
		{
			e.printStackTrace();
		}
	}

	private void exchangeNames()
	{
		// Send my name
		try
		{
			dos.writeUTF(name);
		}
		catch(Exception ex1)
		{
			ex1.printStackTrace();
		}
		try // Read opponent's name
		{
			opponentsName = dis.readUTF();
			if(opponentsName.length() == 0) // If opponent didn't enter anything
			{
				opponentsName = "Opponent";
			}
		}
		catch(IOException ex2)
		{
			ex2.printStackTrace();
		}
	}


	@Override
	public void mouseClicked(MouseEvent e) {}

	@Override
	public void mousePressed(MouseEvent e)
	{
		if(menu == 0 && menuSelectionBox != 0) // In menu
		{
			if(menuSelectionBox == 4)
			{
				System.exit(0);
			}
			menu = menuSelectionBox;
		}
		else if(menu == 1 || menu == 2) // VS Bot or Player
		{
			if(gameState != 0)
			{
				Sah.initialize();
				resetGame();
			}
			else
			{
				if(e.getButton() == MouseEvent.BUTTON1)
				{
					if(selectedRow != -1 && selectedCol != -1) // User clicked twice -> move figure
					{
						int[] destRowCol = calculateMouseRowCol();

						// This also updates board variable for some reason lmao, also it changes the turn
						boolean successfulMove = Sah.moveFigure(selectedRow, selectedCol, destRowCol[0],
								destRowCol[1]);

						selectedRow = selectedCol = -1;

						if(menu == 1 && successfulMove)
						{
							Sah.botMove();
						}
					}
					else
					{
						int[] mouseRowCol = calculateMouseRowCol();
						selectedRow = mouseRowCol[0];
						selectedCol = mouseRowCol[1];
					}
				}
				else if(e.getButton() == MouseEvent.BUTTON3) // Right click!
				{
					selectedRow = selectedCol = -1;
				}
			}
		}
		else if(menu == 3) // LAN Game
		{
			if(LANSetup && mouseX >= OKButtonX && mouseX <= OKButtonX + OKButtonWidth && mouseY >= OKButtonY && mouseY <= OKButtonY + OKButtonHeight)
			{
				if(checkLANInput()) // If user entered syntactically correct input
				{
					name = nameTF.getText();
					ip = ipTF.getText();
					port = Integer.parseInt(portTF.getText());

					if(!connect())
					{
						initializeServer();
						System.out.println("Waiting for client...");
					}
					if(!connected)
					{
						// The one who created the server, now waits for the client
						listenForServerRequest(); // This method BLOCKS execution
					}

					// At this point they are both connected
					exchangeNames();
					Sah.startNetworkThread();

					this.remove(nameTF);
					this.remove(ipTF);
					this.remove(portTF);
					LANSetup = false;
					selectedRow = selectedCol = -1;
				}
				else
				{
					JOptionPane.showMessageDialog(this, "Narobe, vnesite ponovno...");
				}
			}
			else if(!LANSetup) // LAN Game in progress
			{
				if(gameState != 0)
				{
					// Send RESTART string
					try
					{
						dos.writeUTF("RESTART");
					}
					catch(IOException ex)
					{
						ex.printStackTrace();
					}

					sentRestart = true;

					if(opponentRestarted)
					{
						Sah.initialize();
						resetGame();
					}
				}
				else if(turn == 0) // It's this player's turn
				{
					if(e.getButton() == MouseEvent.BUTTON1)
					{
//						if(!LANSetup && selectedRow != -1 && selectedCol != -1) // !LANSetup has to be here...
						if(selectedRow != -1 && selectedCol != -1)
						{
							int[] destRowCol = calculateMouseRowCol();

							if(Sah.checkMove(player, turn, selectedRow, selectedCol, destRowCol[0], destRowCol[1]))
							{
								// Send the move
								try
								{
									dos.writeUTF(selectedRow + "" + selectedCol + "" + destRowCol[0] + "" + destRowCol[1]);
								}
								catch(IOException ex)
								{
									ex.printStackTrace();
								}

								// Also changes turn
								Sah.moveFigureNoCheckMove(selectedRow, selectedCol, destRowCol[0], destRowCol[1]);
							}
							else
							{
								setIllegalMove(destRowCol[0], destRowCol[1]);
							}
							selectedRow = selectedCol = -1;
						}
						else
						{
							int[] mouseRowCol = calculateMouseRowCol();
							selectedRow = mouseRowCol[0];
							selectedCol = mouseRowCol[1];
						}
					}
					else if(e.getButton() == MouseEvent.BUTTON3) // right click!
					{
						selectedRow = selectedCol = -1;
					}
				}
			}
		}
	}

	@Override
	public void mouseReleased(MouseEvent e) {}

	@Override
	public void mouseEntered(MouseEvent e) {}

	@Override
	public void mouseExited(MouseEvent e) {}

	@Override
	public void mouseDragged(MouseEvent e) {}

	@Override
	public void mouseMoved(MouseEvent e)
	{
		// This is needed so coordiantes are relative to JPanel insted of JFrame
		MouseEvent evt = SwingUtilities.convertMouseEvent(e.getComponent(), e, this);
		mouseX = evt.getX();
		mouseY = evt.getY();
	}
}
