package ir;

public class StopWatch {
	private long startTime = System.nanoTime();
	private String description;

	public StopWatch(String description) {
		this.description = description;
	}

	public long getDurationInMilliseconds() {
		return (System.nanoTime() - startTime) / 1_000_000;
	}

	public String toString() {
		return description + " " + getDurationInMilliseconds() + "ms";
	}
}