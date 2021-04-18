import javax.swing.*;
import java.io.IOException;

public class Sah extends JFrame implements Runnable
{
	public static final int WIDTH = 1152, HEIGHT = 900, BOARD_SIZE = 800;
	private static SahPainter painter;

	// Game logic
	private static char[][] board;
	private static String player; // black | white
	private static int turn; // 0 -> my turn; 1 -> opponents turn
	private static int gameState; // -1 -> nobody won; 1 -> black won; 2 -> white won

	// Figures info
	private static final int nOfFigures = 6;
	//->							   			 Rook, 	   Knight,	 Bishop,   King	  	 Queen,    Pawn
	public static final char[] blackFigures = {'\u265C', '\u265E', '\u265D', '\u265A', '\u265B', '\u265F'};
	public static final char[] whiteFigures = {'\u2656', '\u2658', '\u2657', '\u2654', '\u2655', '\u2659'};
	private static int kingIndex;
	private static boolean boardInverted;
	private static boolean isLANGame;

	private static Thread networkThread;

	// Minimax algo stuff
	private static final int pawn = 10;
	private static final int knight = 30;
	private static final int bishop = 30;
	private static final int rook = 50;
	private static final int queen = 90;
	private static final int king = 900;
	private static final int[] figureValues = {rook, knight, bishop, king, queen, pawn}; // It has to match the figures
	// order
	private static final int nPawns = 8;
	private static final int nKnights = 2;
	private static final int nBishops = 2;
	private static final int nRooks = 2;
	private static final int nQueens = 1;
	private static final int nKings = 1;
	private static final int[] nOfEachFiugre = {nRooks, nKnights, nBishops, nKings, nQueens, nPawns};

	// TODO: 18/04/2021 MINIMAX: TRY CAPTRUTING THE LAST MOVED PIECE FIRST, THEN CHECK EVERYTHING ELSE 
	// TODO: 18/04/2021 WIN IS BUGGED AGAINST BOT


	public static void main(String[] args)
	{
		new Sah();
	}

	public Sah()
	{
		new JFrame();
		this.setTitle("Sah");
		this.setSize(WIDTH, HEIGHT);
		this.setResizable(false);
		this.setLocationRelativeTo(null);
		this.setVisible(true);
		this.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

		painter = new SahPainter(board, player); // Any changes to board object here, will also apply to the painter
		// class

		kingIndex = 3;
		boardInverted = false;

		this.add(painter); // Also has MouseAdapter
		this.addMouseListener(painter); // Clicking
		this.addMouseMotionListener(painter); // Moving

		networkThread = new Thread(this);
		isLANGame = false;

		initialize();

		while(true)
		{
			painter.repaint();

			try
			{
				Thread.sleep(10);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}

	public static void initialize()
	{
		gameState = 0;
		board = new char[8][8];
		for(int i = 0; i < 8; i++)
		{
			for(int j = 0; j < 8; j++)
			{
				board[i][j] = ' ';
			}
		}
		for(int i = 0; i < 8; i++)
		{
			int index = i > 4 ? 7 - i : i; // Repetitions for bishop, knight, rook
			board[0][i] = whiteFigures[index];
			board[1][i] = whiteFigures[5];
			board[6][i] = blackFigures[5];
			board[7][i] = blackFigures[index];
		}

		if(boardInverted)
		{
			rotateBoard();
		}

		painter.setBoard(board);
	}

	@Override
	public void run() // networkThread is running here
	{
		while(true)
		{
			if(gameState != 0 && !painter.getOpponentRestarted()) // Someone won and opponent hasn't restarted yet
			{
				try
				{
					String opponentRestarted = painter.getDis().readUTF();
					if(opponentRestarted.equals("RESTART")) // Double check
					{
						painter.setOpponentRestarted(true);
					}

					if(painter.getSentRestart()) // If this user already sent restart, then both have sent restart
					{
						initialize();
						SahPainter.resetGame();
					}
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}
			else if(turn == 1) // Game is in progress
			{
				try
				{
					String opponentMove = painter.getDis().readUTF();
					if(!opponentMove.equals("RESTART")) // Just in case
					{
						// Invert enemy move, move the piece, change turn
						int sourceRow = 7 - ((int) (opponentMove.charAt(0)) - 48);
						int sourceCol = 7 - ((int) (opponentMove.charAt(1)) - 48);
						int destRow = 7 - ((int) (opponentMove.charAt(2)) - 48);
						int destCol = 7 - ((int) (opponentMove.charAt(3)) - 48);

						moveFigureNoCheckMove(sourceRow, sourceCol, destRow, destCol);
					}
				}
				catch(IOException e)
				{
					e.printStackTrace();
				}
			}

			try
			{
				Thread.sleep(100);
			}
			catch(InterruptedException e)
			{
				e.printStackTrace();
			}
		}
	}


	private static char[] getOpponentsFigures(int turn)
	{
		return turn == 0 ? whiteFigures : blackFigures;
	}

	private static char[] getMyFigures(int turn)
	{
		return turn == 0 ? blackFigures : whiteFigures;
	}


	public static boolean checkMove(String player, int turn, int sourceRow, int sourceCol, int destRow, int destCol)
	{
		char sourceFigure = board[sourceRow][sourceCol];
		char destFigure = board[destRow][destCol];

		if(sourceFigure == ' ')
		{
			return false;
		}

		if(isLANGame)
		{
			for(char f : (player.equals("black") ? whiteFigures : blackFigures)) // Iterate through opponent's figures
			{
				if(sourceFigure == f) // User wants to move opponent's figure
				{
					return false;
				}
			}
			// At this point the source figure is ally figure
			for(char f : (player.equals("black") ? blackFigures : whiteFigures)) // Iterate trough all my figures
			{
				if(destFigure == f) // Wants to capture himself
				{
					return false;
				}
			}
		}
		else
		{
			for(char f : (turn == 0 ? whiteFigures : blackFigures)) // Iterate through all opponent's figures
			{
				if(sourceFigure == f) // User wants to move opponent's figure
				{
					return false;
				}
			}
			// At this point the source figure is ally figure
			for(char f : (turn == 0 ? blackFigures : whiteFigures)) // Iterate trough all my figures
			{
				if(destFigure == f) // Wants to capture himself
				{
					return false;
				}
			}
		}

		return checkFigure(player, turn, sourceFigure, destFigure, sourceCol, sourceRow, destCol, destRow);
	}


	private static boolean checkFigure(String player, int turn, char figure, char destFigura, int sourceCol,
									   int sourceRow, int destCol, int destRow)
	{
		int index = kingIndex; // Deafult is king
		if(isLANGame)
		{
			for(int i = 0; i < nOfFigures; i++) // Iterate through all figures
			{
				char f = (player.equals("black") ? blackFigures : whiteFigures)[i]; // Current player's figure
				if(figure == f)
				{
					index = i;
					break;
				}
			}
		}
		else
		{
			for(int i = 0; i < nOfFigures; i++) // Iterate through all figures
			{
				char f = (turn == 0 ? blackFigures : whiteFigures)[i]; // Current player's figure
				if(figure == f)
				{
					index = i;
					break;
				}
			}
		}

		// Check if there is another figure in the way
		if(index != 1) // If its not knight
		{
			int range = Math.max(Math.abs(sourceCol - destCol), Math.abs(sourceRow - destRow));
			// Integer.compare returns on of {-1, 0, 1}, depending on which parameter is greater
			int dirY = Integer.compare(destRow, sourceRow);
			int dirX = Integer.compare(destCol, sourceCol);
			// Illegal diagonal move (angle is not 45 deg)
			if(dirY != 0 && dirX != 0 && Math.abs(sourceCol - destCol) != Math.abs(sourceRow - destRow))
			{
				return false;
			}
			for(int i = 1; i < range; i++)
			{
				int checkY = sourceRow + i * dirY;
				int checkX = sourceCol + i * dirX;
				if(board[checkY][checkX] != ' ')
				{
					return false;
				}
			}
		}

		//Indecies: Rook, Knight, Bishop, King, Queen, Pawn
		if(index == 5) // Pawn
		{
			int maxDistance;
			if(!boardInverted) // Consider both sides (upper and lower)
			{
				// User wants to move back OR to the side for more than 1
				if((turn == 0 ? destRow >= sourceRow : destRow <= sourceRow) || Math.abs(sourceCol - destCol) > 1)
				{
					return false;
				}
				// Determine whether pawn is on his starting position
				maxDistance = (turn == 0 ? (sourceRow == 6 ? 2 : 1) : (sourceRow == 1 ? 2 : 1));
			}
			else // (LAN) Board is inverted: the one that is on the bottom side plays
			{
				// User wants to move back OR to the side for more than 1
				if((player.equals("white") ? destRow >= sourceRow : destRow <= sourceRow) || Math.abs(sourceCol - destCol) > 1)
				{
					return false;
				}

				// Determine whether pawn is on his starting position
				maxDistance = player.equals("white") ? sourceRow == 6 ? 2 : 1 : sourceRow == 1 ? 2 : 1;
			}

			// If users wants to go too far OR field is already taken
			if(sourceCol == destCol && (Math.abs(sourceRow - destRow) > maxDistance || destFigura != ' '))
			{
				return false;
			}

			// If user doesn't go diagonally OR if he goes diagonally, field has to be taken (maxDistance = 1)
			if(!(Math.abs(sourceCol - destCol) != 1 || (destFigura != ' ' && Math.abs(sourceRow - destRow) <= 1)))
			{
				return false;
			}
		}
		else if(index == kingIndex)
		{
			if(!(Math.abs(sourceCol - destCol) <= 1 && Math.abs(sourceRow - destRow) <= 1))
			{
				return false;
			}
		}
		else if(index == 0) // Rook
		{
			if(!(sourceCol == destCol || sourceRow == destRow))
			{
				return false;
			}
		}
		else if(index == 1) // Knight
		{
			if(!(Math.abs(sourceCol - destCol) == 1 && Math.abs(sourceRow - destRow) == 2 || Math.abs(sourceCol - destCol) == 2 && Math.abs(sourceRow - destRow) == 1))
			{
				return false;
			}
		}
		else if(index == 2) // Bishop
		{
			if(!(Math.abs(sourceCol - destCol) == Math.abs(sourceRow - destRow)))
			{
				return false;
			}
		}
		else // Queen (Rook || Bishop)
		{
			if(!(sourceCol == destCol || sourceRow == destRow || Math.abs(sourceCol - destCol) == Math.abs(sourceRow - destRow)))
			{
				return false;
			}
		}

		// Check if this move results in a check
		char tempFigure = board[destRow][destCol];
		// Edge case fix: When opponent was checked, he could respond by checking
		// If we want to capture the king, which isn't possible in a legal move, so I can return true and avoid
		// the isCheck() method
		if(tempFigure == blackFigures[kingIndex] || tempFigure == whiteFigures[kingIndex])
		{
			return true;
		}

		// Move figure
		board[destRow][destCol] = figure;
		board[sourceRow][sourceCol] = ' ';
		int check = isCheck();
		// Reset board
		board[sourceRow][sourceCol] = figure;
		board[destRow][destCol] = tempFigure;

		if(check == 2) // Edge case: Both are in check
		{
			return false;
		}

		if(isLANGame)
		{
			turn = player.equals("black") ? 0 : 1;
		}
		return check != turn;
	}

	/**
	 * @return: number of player, of which king is in check (-1: no check, 0: black, 1: white)
	 */
	private static int isCheck()
	{
		int checkedKing = -1;
		for(int y = 0; y < 8; y++)
		{
			for(int x = 0; x < 8; x++)
			{
				char king = board[y][x];
				if(king == blackFigures[kingIndex] || king == whiteFigures[kingIndex])
				{
					int kingOfPlayer = king == blackFigures[kingIndex] ? 0 : 1;
					if(isFieldUnderAttack(kingOfPlayer, y, x))
					{
						if(checkedKing != -1)
						{
							return 2;
						}
						checkedKing = kingOfPlayer;
					}
				}
			}
		}
		return checkedKing;
	}


	private static boolean isCheckmate(String player, int checkedKing)
	{
		// DEFINITION: When all moves result in check for that player -> checkmate
		char[] myFigures = getMyFigures(checkedKing);

		for(int y = 0; y < 8; y++)
		{
			for(int x = 0; x < 8; x++)
			{
				char figure = board[y][x];
				for(char myFigure : myFigures)
				{
					if(figure == myFigure) // Found our figure
					{
						for(int i = 0; i < 8; i++)
						{
							for(int j = 0; j < 8; j++)
							{
								if((y != i || x != j) && checkMove(player, checkedKing, y, x, i, j)) // This figure
								// can make a move
								{
									char destFigure = board[i][j];

									board[i][j] = board[y][x];
									board[y][x] = ' ';
									int check = isCheck();

									board[y][x] = board[i][j];
									board[i][j] = destFigure;

									if(check == -1) // If this move does NOT result in a check -> no checkmate
									{
										return false;
									}
								}
							}
						}
					}
				}
			}
		}
		return true;
	}


	private static boolean isFieldUnderAttack(int kingOfPlayer, int y, int x)
	{
		char[] opponentsFigures = getOpponentsFigures(kingOfPlayer);

		for(int i = 0; i < 8; i++)
		{
			for(int j = 0; j < 8; j++)
			{
				char figure = board[i][j];
				for(char nFigure : opponentsFigures)
				{
					String opponentPlayer = kingOfPlayer == 0 ? "white" : "black";
					if(figure == nFigure && checkMove(opponentPlayer, 1 - kingOfPlayer, i, j, y, x))
					{
						return true;
					}
				}
			}
		}
		return false;
	}

	public static boolean moveFigure(int sourceRow, int sourceCol, int destRow, int destCol)
	{
		if(checkMove(player, turn, sourceRow, sourceCol, destRow, destCol)) //TODO: Dont check here, only move the ...
		// ... figure on the board!
		{
			board[destRow][destCol] = board[sourceRow][sourceCol];
			board[sourceRow][sourceCol] = ' ';

			gameState = checkWin();
			if(gameState != 0)
			{
				painter.setWinner(gameState);
			}

			turn = 1 - turn;
			painter.setTurn(turn);
			return true;
		}
		painter.setIllegalMove(destRow, destCol); // Visual feedback that this move is illegal
		return false;
	}

	public static void moveFigureNoCheckMove(int sourceRow, int sourceCol, int destRow, int destCol)
	{
		board[destRow][destCol] = board[sourceRow][sourceCol];
		board[sourceRow][sourceCol] = ' ';

		gameState = checkWin();
		if(gameState != 0)
		{
			painter.setWinner(gameState);
		}

		turn = 1 - turn;
		painter.setTurn(turn);
	}

	private static int checkWin()
	{
		int checkedKing = isCheck();
		if(checkedKing != -1 && checkedKing != 2)
		{
			String checkedPlayer = checkedKing == 0 ? "black" : "white";
			if(isCheckmate(checkedPlayer, checkedKing))
			{
				return 2 - checkedKing; // 1 -> black won, 2 - white won
			}
			int[] kingPosition = findKing(checkedKing);
			painter.setCheckedKingPosition(kingPosition[0], kingPosition[1]);
		}
		else
		{
			painter.setCheckedKingPosition(-1, -1);
		}
		return 0; // No one won
	}

	public static void rotateBoard()
	{
		// y invert
		for(int i = 0; i < 8; i++)
		{
			for(int j = 0; j < 4; j++)
			{
				char tempFigura = board[j][i];
				board[j][i] = board[7 - j][i];
				board[7 - j][i] = tempFigura;
			}
		}

		// x invert
		for(int i = 0; i < 8; i++)
		{
			for(int j = 0; j < 4; j++)
			{
				char tempFigura = board[i][j];
				board[i][j] = board[i][7 - j];
				board[i][7 - j] = tempFigura;
			}
		}
		boardInverted = true;
	}

	private static int[] findKing(int checkedKing)
	{
		for(int y = 0; y < 8; y++)
		{
			for(int x = 0; x < 8; x++)
			{
				char king = board[y][x];

				if(king == (checkedKing == 0 ? blackFigures[kingIndex] : whiteFigures[kingIndex]))
				{
					return new int[]{y, x};
				}
			}
		}
		return new int[]{-1, -1};
	}


	private static int evaluateBoard(int turn)
	{
		//Figure Indices: Rook, Knight, Bishop, King, Queen, Pawn
		int score = 0;
		int[] figuresCounter = new int[nOfFigures]; // How many figures are currently on the board
		char[] myFigures = getMyFigures(turn);
		for(int i = 0; i < 8; i++)
		{
			for(int j = 0; j < 8; j++)
			{
				char figure = board[i][j];
				for(int k = 0; k < myFigures.length; k++)
				{
					if(figure == myFigures[k])
					{
						figuresCounter[k]++;
						score += figureValues[k];
					}
				}
			}
		}

		// If a figure is missing, we have to subtract its value
		for(int i = 0; i < figuresCounter.length; i++)
		{
			int diff = nOfEachFiugre[i] - figuresCounter[i];
			score -= diff * figureValues[i];
		}

		return score;
	}


	public static void botMove()
	{
		long last = System.currentTimeMillis();
		char[] botFigures = getMyFigures(turn);
		int bestScore = -Integer.MAX_VALUE;
		int[] bestMove = new int[4];

		for(int y = 0; y < 8; y++)
		{
			for(int x = 0; x < 8; x++)
			{
				char figure = board[y][x];
				if(figure == ' ')
				{
					continue;
				}
				for(char f : botFigures)
				{
					if(figure == f)
					{
						for(int i = 0; i < 8; i++)
						{
							for(int j = 0; j < 8; j++)
							{
								if(checkMove("white", turn, y, x, i, j))
								{
									char temp = board[i][j];
									board[i][j] = board[y][x];
									board[y][x] = ' ';

									int score = minimax(3, -Integer.MAX_VALUE, Integer.MAX_VALUE, false);
									if(score > bestScore)
									{
										bestScore = score;
										bestMove = new int[]{y, x, i, j};
									}

									board[y][x] = board[i][j];
									board[i][j] = temp;
								}
							}
						}
						break;
					}
				}
			}
		}
		moveFigureNoCheckMove(bestMove[0], bestMove[1], bestMove[2], bestMove[3]);
		System.err.println(System.currentTimeMillis() - last); // TODO: 18/04/2021 DELETE? 
	}

	private static int minimax(int depth, int alpha, int beta, boolean isMaximizing)
	{
		if(depth == 0)
		{
			return evaluateBoard(turn);
		}

		int score;
		String player = isMaximizing ? "white" : "black";
		int turn = isMaximizing ? 1 : 0;
		char[] myFigures = getMyFigures(turn);

		if(isMaximizing)
		{
			score = -Integer.MAX_VALUE;

			for(int y = 0; y < 8; y++)
			{
				for(int x = 0; x < 8; x++)
				{
					char figure = board[y][x];
					if(figure == ' ')
					{
						continue;
					}
					for(char f : myFigures)
					{
						if(figure == f)
						{
							for(int i = 0; i < 8; i++)
							{
								for(int j = 0; j < 8; j++)
								{
									if(checkMove(player, turn, y, x, i, j))
									{
										char temp = board[i][j];
										board[i][j] = board[y][x];
										board[y][x] = ' ';

										score = Math.max(score, minimax(depth - 1, alpha, beta, false));
										alpha = Math.max(alpha, score);

										board[y][x] = board[i][j];
										board[i][j] = temp;

										if(beta <= alpha)
										{
											return score;
										}
									}
								}
							}
							break;
						}
					}
				}
			}
		}
		else
		{
			score = Integer.MAX_VALUE;

			for(int y = 0; y < 8; y++)
			{
				for(int x = 0; x < 8; x++)
				{
					char figure = board[y][x];
					if(figure == ' ')
					{
						continue;
					}
					for(char f : myFigures)
					{
						if(figure == f)
						{
							for(int i = 0; i < 8; i++)
							{
								for(int j = 0; j < 8; j++)
								{
									if(checkMove(player, turn, y, x, i, j))
									{
										char temp = board[i][j];
										board[i][j] = board[y][x];
										board[y][x] = ' ';

										score = Math.min(score, minimax(depth - 1, alpha, beta, true));
										beta = Math.min(beta, score);

										board[y][x] = board[i][j];
										board[i][j] = temp;

										if(beta <= alpha)
										{
											return score;
										}
									}
								}
							}
							break;
						}
					}
				}
			}
		}
		return score;
	}

	public static void setPlayer(String p)
	{
		player = p;
	}

	public static void setTurn(int t)
	{
		turn = t;
	}

	public static void startNetworkThread()
	{
		networkThread.start();
		isLANGame = true;
	}
}
