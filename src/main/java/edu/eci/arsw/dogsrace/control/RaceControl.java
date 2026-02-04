package edu.eci.arsw.dogsrace.control;

/**
 * Common monitor to pause/resume all runners.
 *
 * Uses wait()/notifyAll() as requested by the lab.
 */
public final class RaceControl {

    private final Object monitor = new Object();
    private boolean paused = false;

    public void pause() {
        synchronized (monitor) {
            paused = true;
        }
    }

    public void resume() {
        synchronized (monitor) {
            paused = false;
            monitor.notifyAll();
        }
    }

    public boolean isPaused() {
        synchronized (monitor) {
            return paused;
        }
    }

    /**
     * Call frequently from the running threads to honor pause/resume.
     */
    public void awaitIfPaused() throws InterruptedException {
        synchronized (monitor) {
            while (paused) {
                monitor.wait();
            }
        }
    }
}
