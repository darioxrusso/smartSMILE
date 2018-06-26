package domoNetWS.techManager.bticinoManager;

public class ParserOpen {
	static final int _EOF = 0;
	static final int _digit = 1;
	static final int maxT = 7;

	static final boolean T = true;
	static final boolean x = false;
	static final int minErrDist = 2;

	public Token t; // last recognized token
	public Token la; // lookahead token
	int errDist = minErrDist;

	public ScannerOpen scanner;
	public Errors errors;

	public ParserOpen(ScannerOpen scanner) {
		this.scanner = scanner;
		errors = new Errors();
	}

	void SynErr(int n) {
		if (errDist >= minErrDist)
			errors.SynErr(la.line, la.col, n);
		errDist = 0;
	}

	public void SemErr(String msg) {
		if (errDist >= minErrDist)
			errors.SemErr(t.line, t.col, msg);
		errDist = 0;
	}

	void Get() {
		for (;;) {
			t = la;
			la = scanner.Scan();
			if (la.kind <= maxT) {
				++errDist;
				break;
			}

			la = t;
		}
	}

	void Expect(int n) {
		if (la.kind == n)
			Get();
		else {
			SynErr(n);
		}
	}

	boolean StartOf(int s) {
		return set[s][la.kind];
	}

	void ExpectWeak(int n, int follow) {
		if (la.kind == n)
			Get();
		else {
			SynErr(n);
			while (!StartOf(follow))
				Get();
		}
	}

	boolean WeakSeparator(int n, int syFol, int repFol) {
		int kind = la.kind;
		if (kind == n) {
			Get();
			return true;
		} else if (StartOf(repFol))
			return false;
		else {
			SynErr(n);
			while (!(set[syFol][kind] || set[repFol][kind] || set[0][kind])) {
				Get();
				kind = la.kind;
			}
			return StartOf(syFol);
		}
	}

	void SerieComandi() {
		Comando();
		while (la.kind == 2) {
			Comando();
		}
	}

	void Comando() {
		Expect(2);
		if (la.kind == 1) {
			chi();
			Expect(2);
			cosa();
			Expect(2);
			param();
		} else if (la.kind == 3) {
			Get();
			chi();
			while (la.kind == 2) {
				Get();
				param();
			}
		} else if (la.kind == 4) {
			Get();
		} else if (la.kind == 5) {
			Get();
		} else
			SynErr(8);
		Expect(6);
	}

	void chi() {
		number();
	}

	void cosa() {
		number();
		while (la.kind == 3) {
			Get();
			number();
		}
	}

	void param() {
		if (la.kind == 1) {
			number();
		}
		while (la.kind == 3) {
			Get();
			number();
		}
	}

	void number() {
		Expect(1);
		while (la.kind == 1) {
			Get();
		}
	}

	public void Parse() {
		la = new Token();
		la.val = "";
		Get();
		SerieComandi();

		Expect(0);
	}

	private boolean[][] set = { { T, x, x, x, x, x, x, x, x }

	};
} // end Parser

class Errors {
	public int count = 0; // number of errors detected
	public java.io.PrintStream errorStream = System.out; // error messages go to
															// this stream
	public String errMsgFormat = "-- line {0} col {1}: {2}"; // 0=line,
																// 1=column,
																// 2=text

	protected void printMsg(int line, int column, String msg) {
		StringBuffer b = new StringBuffer(errMsgFormat);
		int pos = b.indexOf("{0}");
		if (pos >= 0) {
			b.delete(pos, pos + 3);
			b.insert(pos, line);
		}
		pos = b.indexOf("{1}");
		if (pos >= 0) {
			b.delete(pos, pos + 3);
			b.insert(pos, column);
		}
		pos = b.indexOf("{2}");
		if (pos >= 0)
			b.replace(pos, pos + 3, msg);
		errorStream.println(b.toString());
	}

	public void SynErr(int line, int col, int n) {
		String s;
		switch (n) {
		case 0:
			s = "EOF expected";
			break;
		case 1:
			s = "digit expected";
			break;
		case 2:
			s = "\"*\" expected";
			break;
		case 3:
			s = "\"#\" expected";
			break;
		case 4:
			s = "\"#*1\" expected";
			break;
		case 5:
			s = "\"#*0\" expected";
			break;
		case 6:
			s = "\"##\" expected";
			break;
		case 7:
			s = "??? expected";
			break;
		case 8:
			s = "invalid Comando";
			break;
		default:
			s = "error " + n;
			break;
		}
		printMsg(line, col, s);
		count++;
	}

	public void SemErr(int line, int col, String s) {
		printMsg(line, col, s);
		count++;
	}

	public void SemErr(String s) {
		errorStream.println(s);
		count++;
	}

	public void Warning(int line, int col, String s) {
		printMsg(line, col, s);
	}

	public void Warning(String s) {
		errorStream.println(s);
	}
} // Errors

class FatalError extends RuntimeException {
	public static final long serialVersionUID = 1L;

	public FatalError(String s) {
		super(s);
	}
}
