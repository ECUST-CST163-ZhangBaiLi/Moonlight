package zbl.moonlight.client;

import zbl.moonlight.client.common.Command;
import zbl.moonlight.client.exception.ClientRunException;
import zbl.moonlight.client.exception.InvalidCommandException;
import zbl.moonlight.client.exception.InvalidMethodException;
import zbl.moonlight.client.thread.Executor;
import zbl.moonlight.server.protocol.ResponseCode;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.net.Socket;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import java.util.concurrent.ConcurrentLinkedQueue;

public class MoonlightClient {
    private final String host;
    private final int port;
    private DataInputStream inputStream;
    private DataOutputStream outputStream;
    private Scanner scanner;

    private final ConcurrentLinkedQueue<Command> queue;

    public MoonlightClient(String host, int port) {
        this(host, port, null);
    }

    public MoonlightClient(String host, int port, ConcurrentLinkedQueue<Command> queue) {
        this.host = host;
        this.port = port;
        this.queue = queue;
    }

    public void run() throws IOException, ClientRunException {
        if(queue == null) {
            throw new ClientRunException("commands queue is null");
        }

        init();
        new Thread(new Executor(queue), "client-executor").start();
    }

    public void send(String str) {
        try {
            queue.offer(new Command(str));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void runInTerminal() throws IOException {
        init();
        scanner = new Scanner(System.in);

        while (true) {
            try {
                print();
                Command command = readLine();
                send(command);
                showResponse();
            } catch (InvalidMethodException e) {
                printError("Method", e.getMessage());
                continue;
            } catch (InvalidCommandException e) {
                printError("Command", e.getMessage());
                continue;
            }
        }
    }

    public static void main(String[] args) throws IOException {
        MoonlightClient client = new MoonlightClient("127.0.0.1", 7820);
        client.runInTerminal();
    }

    private void init() throws IOException {
        Socket socket = new Socket(host, port);
        inputStream = new DataInputStream(new BufferedInputStream(socket.getInputStream()));
        outputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
    }

    private Command readLine() throws InvalidMethodException, InvalidCommandException {
        return new Command(scanner.nextLine());
    }

    private void print() {
        System.out.print("Moonlight > ");
    }

    private void printError(String type, String message) {
        System.out.println("[Invalid " + type + "][" + message + "]");
    }

    private void send(Command command) throws IOException, InvalidCommandException {
        byte method = command.getCode();
        byte[] key = command.getKey().toString().getBytes(StandardCharsets.UTF_8);
        byte[] value = command.getValue().toString().getBytes(StandardCharsets.UTF_8);
        if(key.length > 255) {
            throw new InvalidCommandException("key is too long.");
        }
        byte keyLength = (byte) key.length;
        int valueLength = value.length;

        outputStream.write(new byte[]{method, keyLength});
        outputStream.writeInt(valueLength);
        outputStream.write(key);
        outputStream.write(value);
        outputStream.flush();
    }

    private void showResponse() throws IOException {
        byte responseCode = inputStream.readByte();
        int valueLength = inputStream.readInt();
        String responseValue = "";
        if(valueLength != 0) {
            byte[] responseValueBytes = new byte[valueLength];
            inputStream.read(responseValueBytes);
            responseValue += new String(responseValueBytes);
        }
        System.out.println("[" + ResponseCode.getCodeName(responseCode) + "][" + valueLength + "][" + responseValue + "]");
    }
}
