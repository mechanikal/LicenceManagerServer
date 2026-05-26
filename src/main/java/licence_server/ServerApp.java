package licence_server;

import common.MulticastData;
import licence_server.licence.ActiveLicenceListener;
import common.Records;
import licence_server.network.ServerCore;

import javax.swing.*;
import javax.swing.Timer;
import java.awt.*;
import java.net.UnknownHostException;
import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ServerApp {
    private ServerCore server;
    private final JFrame frame;
    Map<String, List<Records.LicenceData>> activeLicences;
    private final JTextArea discoveryRequestsTextArea;
    private final JTextArea activeLicencesTextArea;
    private final ActiveLicenceListener activeLicenceListener;
    private final BlockingQueue<String> discoveryRequestQueue = new LinkedBlockingQueue<>();
    private final JLabel mlsPortLabel;
    private volatile boolean serverRunning = false;
    Timer activelicencesRefreshTimer = new Timer(1000, e -> refreshActiveLicences());
    Thread discoveryRequestPollThread = new Thread(this::discoveryRequestRefresher);

    public ServerApp() {
        frame = new JFrame("Licence Server");
        JPanel panel = new JPanel(new GridBagLayout());
        GridBagConstraints gridConstraints = new GridBagConstraints();

        JButton closeServerButton = new JButton("Close server");
        closeServerButton.addActionListener(_ -> closeServer());
        activeLicencesTextArea = new JTextArea();
        activeLicencesTextArea.setEditable(false);
        discoveryRequestsTextArea = new JTextArea();
        discoveryRequestsTextArea.setEditable(false);
        mlsPortLabel = new JLabel("TCP port:");
        JLabel discoveryLabel = new JLabel("discovery requests:");
        JLabel activeLicencesLabel = new JLabel("active licences:");
        JScrollPane activeLicencesScrollPane = new JScrollPane(activeLicencesTextArea);
        JScrollPane discoveryRequestsScrollPane = new JScrollPane(discoveryRequestsTextArea);

        gridConstraints.gridx = 0;
        gridConstraints.gridy = 0;
        gridConstraints.gridwidth = 2;
        gridConstraints.weighty = 0;
        gridConstraints.weightx = 1;
        panel.add(mlsPortLabel, gridConstraints);


        gridConstraints.gridx = 0;
        gridConstraints.gridy = 1;
        gridConstraints.gridwidth = 1;
        gridConstraints.weighty = 0;
        panel.add(activeLicencesLabel, gridConstraints);

        gridConstraints.gridx = 1;
        gridConstraints.gridy = 1;
        gridConstraints.gridwidth = 1;
        gridConstraints.weighty = 0;
        panel.add(discoveryLabel, gridConstraints);

        gridConstraints.gridx = 0;
        gridConstraints.gridy = 2;
        gridConstraints.gridwidth = 1;
        gridConstraints.weighty = 1;
        gridConstraints.fill = GridBagConstraints.BOTH;
        panel.add(activeLicencesScrollPane, gridConstraints);

        gridConstraints.gridx = 1;
        gridConstraints.gridy = 2;
        gridConstraints.gridwidth = 1;
        gridConstraints.weighty = 1;
        gridConstraints.fill = GridBagConstraints.BOTH;
        panel.add(discoveryRequestsScrollPane, gridConstraints);

        gridConstraints.gridx = 0;
        gridConstraints.gridy = 3;
        gridConstraints.gridwidth = 2;
        gridConstraints.weighty = 0;
        panel.add(closeServerButton, gridConstraints);

        frame.add(panel);

        frame.setSize(500, 300);
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);

        activeLicenceListener = new ActiveLicenceListener() {
            @Override
            public void update(Map<String, List<Records.LicenceData>> licences) {
                SwingUtilities.invokeLater(() -> { updateActiveLicences(licences); });
            }
        };
    }

    public void startServer() {
        while (true) {
            String port = JOptionPane.showInputDialog(
                    null,"input server port:", "server port", JOptionPane.PLAIN_MESSAGE
            );
            try {
                if (port == null) {
                    return;
                }
                int portInt = Integer.parseInt(port);
                if (portInt == MulticastData.multicastPort){
                    showError("port reserved");
                    continue;
                }
                server = new ServerCore(portInt,activeLicenceListener,discoveryRequestQueue);
                server.run();
                frame.setVisible(true);
                activelicencesRefreshTimer.start();
                serverRunning = true;
                discoveryRequestPollThread.start();
                mlsPortLabel.setText("TCP port: "+portInt);
                return;
            } catch (NumberFormatException _) {
                showError("port must be an integer");
            } catch (IllegalArgumentException _) {
                showError("port invalid");
            } catch (UnknownHostException e) {
                showError("cant resolve host");
            }
        }
    }

    private void showError(String message) {
        JOptionPane.showMessageDialog(
                null,
                message,
                "Error",
                JOptionPane.ERROR_MESSAGE
        );
    }

    public static void main(String[] args) {
        ServerApp gui = new ServerApp();
        gui.startServer();
    }

    private void closeServer() {
        server.stop();
        frame.dispose();
        serverRunning = false;
        activelicencesRefreshTimer.stop();
    }

    private List<String> activeLicencesToString(Instant now) {
        if (activeLicences == null || activeLicences.isEmpty()) {
            return null;
        }
        java.util.List<String> result = new ArrayList<>();
        Set<String> users = activeLicences.keySet();
        for (String User : users) {
            StringBuilder stringBuilder = new StringBuilder();
            List<Records.LicenceData> licences = activeLicences.get(User);
            if (licences == null) {
                continue;
            }
            if (licences.isEmpty()) {
                continue;
            }
            stringBuilder.append(licences.size()).append(" licences for ").append(User).append(":\n");
            for (Records.LicenceData licence : licences) {
                Long timeRemaining = Duration.between(now, licence.ExpirationDate()).toSeconds();
                stringBuilder.append("\t").append(licence.ipAddress()).append(" until expiration:").append(timeRemaining).append("\n");
            }
            result.add(stringBuilder.toString());
        }
        return result;
    }

    private void updateActiveLicences(Map<String, List<Records.LicenceData>> licences) {
        this.activeLicences = licences;
    }

    private void refreshActiveLicences() {
        if (activeLicences == null || activeLicences.isEmpty()) {
            activeLicencesTextArea.setText("");
            return;
        }
        Instant now = Instant.now();
        for (List<Records.LicenceData> licences : activeLicences.values()) {
            licences.removeIf(licence -> licence.ExpirationDate().isBefore(now));
        }
        List<String> licencesStringList = activeLicencesToString(now);
        if  (licencesStringList == null) {
            activeLicencesTextArea.setText("");
            return;
        }
        StringBuilder activeLicencesString = new StringBuilder();
        for (String UserLicenceInformation : licencesStringList) {
            activeLicencesString.append(UserLicenceInformation).append("\n");
        }
        activeLicencesTextArea.setText(activeLicencesString.toString());
    }

    private void discoveryRequestRefresher() {
        while(serverRunning){
            try {
                String newRequest = discoveryRequestQueue.poll(1, TimeUnit.SECONDS);
                if (newRequest == null) {
                    continue;
                }
                SwingUtilities.invokeLater(() ->discoveryRequestsTextArea.append("new DISCOVERY request from "+newRequest+"\n"));
            }catch (InterruptedException _){}
        }
    }
}
