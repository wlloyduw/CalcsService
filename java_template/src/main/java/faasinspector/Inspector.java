package faasinspector;

import java.beans.IntrospectionException;
import java.beans.Introspector;
import java.beans.PropertyDescriptor;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.reflect.InvocationTargetException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * SAAF
 *
 * @author Wes Lloyd
 * @author Robert Cordingly
 */
public class Inspector {

    private final HashMap<String, Object> attributes;
    private final long startTime;

    /**
     * Initialize Inspector.
     *
     * attributes: Used to store information collected by each function.
     * version: Inspector version.
     * lang: Function language (java).
     */
    public Inspector() {
        startTime = System.currentTimeMillis();
        attributes = new HashMap<>();

        attributes.put("version", 0.31);
        attributes.put("lang", "java");
    }

    /**
     * Collect information about the runtime container.
     *
     * uuid:         A unique identifier assigned to a container if one does not already exist. 
     * newcontainer: Whether a container is new (no assigned uuid) or if it has been used before. 
     * vmuptime:     The time when the system started in Unix time.
     */
    public void inspectContainer() {

        File f;
        Path p;

        //Stamp Container
        int newContainer;
        String uuid = "";

        f = new File("/tmp/container-id");
        p = Paths.get("/tmp/container-id");
        if (f.exists()) {
            newContainer = 0;
            try (BufferedReader br = Files.newBufferedReader(p)) {
                uuid = br.readLine();
                br.close();
            } catch (IOException ioe) {
                //sError = STAMP_ERR_READING_EXISTING_UUID;
            }
        } else {
            newContainer = 1;
            try (BufferedWriter bw = Files.newBufferedWriter(p,
                    StandardCharsets.US_ASCII, StandardOpenOption.CREATE_NEW)) {
                uuid = UUID.randomUUID().toString();
                bw.write(uuid);
                bw.close();
            } catch (IOException ioe) {
                //sError = STAMP_ERR_WRITING_NEW_UUID;
            }
        }

        attributes.put("uuid", uuid);
        attributes.put("newcontainer", newContainer);

        //Get VM Uptime
        String filename = "/proc/stat";
        f = new File(filename);
        p = Paths.get(filename);
        String text;
        if (f.exists()) {
            try (BufferedReader br = Files.newBufferedReader(p)) {
                while ((text = br.readLine()) != null && text.length() != 0) {
                    // get boot time in ms since epoch 
                    if (text.contains("btime")) {
                        String prms[] = text.split(" ");
                        attributes.put("vmuptime", Long.parseLong(prms[1]));
                    }
                }
                br.close();

            } catch (IOException ioe) {
                //sb.append("Error reading file=" + filename);
            }
        }
    }

    /**
     * Collect information about the CPU assigned to this function.
     *
     * cpuType:    The model name of the CPU. 
     * cpuModel:   The model number of the CPU. 
     * cpuCores:   The total number of cpu cores.
     * cpuUsr:     Time spent normally executing in user mode. 
     * cpuNice:    Time spent executing niced processes in user mode. 
     * cpuKrn:     Time spent executing processes in kernel mode. 
     * cpuIdle:    Time spent idle. 
     * cpuIowait:  Time spent waiting for I/O to complete. 
     * cpuIrq:     Time spent servicing interrupts. 
     * cpuSoftIrq: Time spent servicing software interrupts.
     * vmcpusteal: Time spent waiting for real CPU while hypervisor is using another virtual CPU.
     * contextSwitches: Number of context switches.
     */
    public void inspectCPU() {

        String text;
        int start;
        int end;

        //Get CPU Type
        text = getFileAsString("/proc/cpuinfo");
        start = text.indexOf("name") + 7;
        end = start + text.substring(start).indexOf(":");
        String cpuType = text.substring(start, end - 9).trim();
        attributes.put("cpuType", cpuType);

        //Get CPU Model
        start = text.indexOf("model") + 9;
        end = start + text.substring(start).indexOf(":");
        String cpuModel = text.substring(start, end - 11).trim();
        attributes.put("cpuModel", cpuModel);

        //Get CPU Core Count
        start = text.indexOf("cpu cores") + 12;
        end = start + text.substring(start).indexOf(":");
        String cpuCores = text.substring(start, end - 9).trim();
        attributes.put("cpuCores", cpuCores);

        //Get CPU Metrics
        String filename = "/proc/stat";
        File f = new File(filename);
        Path p = Paths.get(filename);
        if (f.exists()) {
            try (BufferedReader br = Files.newBufferedReader(p)) {
                text = br.readLine();
                String params[] = text.split(" ");

                String[] metricNames = {"cpuUsr", "cpuNice", "cpuKrn", "cpuIdle",
                    "cpuIowait", "cpuIrq", "cpuSoftIrq", "vmcpusteal"};

                for (int i = 0; i < metricNames.length; i++) {
                    attributes.put(metricNames[i], Long.parseLong(params[i + 2]));
                }

                while ((text = br.readLine()) != null && text.length() != 0) {
                    if (text.contains("ctxt")) {
                        String prms[] = text.split(" ");
                        attributes.put("contextSwitches", Long.parseLong(prms[1]));
                    }
                }

                br.close();
            } catch (IOException ioe) {
                //sb.append("Error reading file=" + filename);
            }
        }
    }
    
    /**
     * Compare information gained from inspectCPU to the current CPU metrics.
     *
     * cpuUsrDelta:     Time spent normally executing in user mode. 
     * cpuNiceDelta:    Time spent executing niced processes in user mode. 
     * cpuKrnDelta:     Time spent executing processes in kernel mode. 
     * cpuIdleDelta:    Time spent idle. 
     * cpuIowaitDelta:  Time spent waiting for I/O to complete. 
     * cpuIrqDelta:     Time spent servicing interrupts. 
     * cpuSoftIrqDelta: Time spent servicing software interrupts.
     * vmcpustealDelta: Time spent waiting for real CPU while hypervisor is using another virtual CPU.
     * contextSwitchesDelta: Number of context switches.
     */
    public void inspectCPUDelta() {

        String text;

        //Get CPU Metrics
        String filename = "/proc/stat";
        File f = new File(filename);
        Path p = Paths.get(filename);
        if (f.exists()) {
            try (BufferedReader br = Files.newBufferedReader(p)) {
                text = br.readLine();
                String params[] = text.split(" ");

                String[] metricNames = {"cpuUsr", "cpuNice", "cpuKrn", "cpuIdle",
                    "cpuIowait", "cpuIrq", "cpuSoftIrq", "vmcpusteal"};

                for (int i = 0; i < metricNames.length; i++) {
                    attributes.put(metricNames[i] + "Delta", Long.parseLong(params[i + 2]) - (Long)attributes.get(metricNames[i]));
                }

                while ((text = br.readLine()) != null && text.length() != 0) {
                    if (text.contains("ctxt")) {
                        String prms[] = text.split(" ");
                        attributes.put("contextSwitchesDelta", Long.parseLong(prms[1]) - (Long)attributes.get("contextSwitches"));
                    }
                }

                br.close();
            } catch (IOException ioe) {
                //sb.append("Error reading file=" + filename);
            }
        }
    }

    /**
     * Inspects /proc/meminfo and /proc/vmstat. Add memory specific attributes:
     * 
     * totalMemory:     Total memory allocated to the VM in kB.
     * freeMemory:      Current free memory in kB when inspectMemory is called.
     * pageFaults:      Total number of page faults experienced by the vm since boot.
     * majorPageFaults: Total number of major page faults experienced since boot.
     * 
     */
    public void inspectMemory() {
        String memInfo = getFileAsString("/proc/meminfo");
        String[] lines = memInfo.split("\n");
        attributes.put("totalMemory", lines[0].replace("MemTotal:", "").replace("\t", "").replace(" kB", "").replace(" ", ""));
        attributes.put("freeMemory", lines[1].replace("MemFree:", "").replace("\t", "").replace(" kB", "").replace(" ", ""));

        String text;

        //Get CPU Metrics
        String filename = "/proc/vmstat";
        File f = new File(filename);
        Path p = Paths.get(filename);
        if (f.exists()) {
            try (BufferedReader br = Files.newBufferedReader(p)) {
                while ((text = br.readLine()) != null && text.length() != 0) {
                    if (text.contains("pgfault")) {
                        String prms[] = text.split(" ");
                        attributes.put("pageFaults", Long.parseLong(prms[1]));
                    } else if (text.contains("pgmajfault")) {
                        String prms[] = text.split(" ");
                        attributes.put("majorPageFaults", Long.parseLong(prms[1]));
                    }
                }

                br.close();
            } catch (IOException ioe) {
                //sb.append("Error reading file=" + filename);
            }
        }
    }

    /**
     * Inspects /proc/vmstat to see how specific memory stats have changed.
     * 
     * pageFaultsDelta:     The number of page faults experienced since inspectMemory was called.
     * majorPageFaultsDelta: The number of major pafe faults since inspectMemory was called.
     */
    public void inspectMemoryDelta() {
        String text;

        //Get CPU Metrics
        String filename = "/proc/vmstat";
        File f = new File(filename);
        Path p = Paths.get(filename);
        if (f.exists()) {
            try (BufferedReader br = Files.newBufferedReader(p)) {
                while ((text = br.readLine()) != null && text.length() != 0) {
                    if (text.contains("pgfault")) {
                        String prms[] = text.split(" ");
                        attributes.put("pageFaultsDelta", Long.parseLong(prms[1]) - (Long)attributes.get("pageFaults"));
                    } else if (text.contains("pgmajfault")) {
                        String prms[] = text.split(" ");
                        attributes.put("majorPageFaultsDelta", Long.parseLong(prms[1]) - (Long)attributes.get("majorPageFaults"));
                    }
                }

                br.close();
            } catch (IOException ioe) {
                //sb.append("Error reading file=" + filename);
            }
        }
    }

    /**
     * Collect information about the current FaaS platform.
     *
     * platform:    The FaaS platform hosting this function.
     * containerID: A unique identifier for containers of a platform.
     * vmID:        A unique identifier for virtual machines of a platform.
     */
    public void inspectPlatform() {
        String environment = runCommand(new String[]{"env"});
        if (environment.contains("AWS_LAMBDA")) {
            attributes.put("platform", "AWS Lambda");
            
            String searchTerm = "AWS_LAMBDA_LOG_STREAM_NAME=";
            int logIndex = environment.indexOf(searchTerm);
            int startIndex = logIndex + searchTerm.length();
            int endIndex = startIndex;
            while (environment.charAt(endIndex) != '\n') endIndex++;
            
            attributes.put("containerID", environment.substring(startIndex, endIndex).replace("\n", ""));
                    
            String vmID = runCommand(new String[]{"cat", "/proc/self/cgroup"});
            int index = vmID.indexOf("sandbox-root");
            attributes.put("vmID", vmID.substring(index + 13, index + 19));
            
        } else if (environment.contains("X_GOOGLE")) {
            attributes.put("platform", "Google Cloud Functions");
        } else if (environment.contains("functions.cloud.ibm")) {
            attributes.put("platform", "IBM Cloud Functions");

            attributes.put("vmID", runCommand(new String[]{"cat", "/sys/hypervisor/uuid"}).trim());

        } else if (environment.contains("microsoft.com/azure-functions")) {
            attributes.put("platform", "Azure Functions");
            
            String searchTerm = "CONTAINER_NAME=";
            int logIndex = environment.indexOf(searchTerm);
            int startIndex = logIndex + searchTerm.length();
            int endIndex = startIndex;
            while (environment.charAt(endIndex) != '\n') endIndex++;
            
            attributes.put("containerID", environment.substring(startIndex, endIndex).replace("\n", ""));
            
        } else {
            attributes.put("platform", "Unknown Platform");
        }
    }

    /**
     * Collect information about the linux kernel.
     *
     * linuxVersion: The version of the linux kernel.
     */
    public void inspectLinux() {
        String linuxVersion = runCommand(new String[]{"uname", "-v"}).trim();
        attributes.put("linuxVersion", linuxVersion);
    }

    /**
     * Run all data collection methods and record framework runtime.
     */
    public void inspectAll() {
        this.inspectContainer();
        this.inspectPlatform();
        this.inspectLinux();
        this.inspectMemory();
        this.inspectCPU();
        this.addTimeStamp("frameworkRuntime");
    }

    /**
     * Run all delta collection methods add userRuntime attribute to further isolate
     * use code runtime from time spent collecting data.
     */
    public void inspectAllDeltas() {
        Long currentTime = System.currentTimeMillis();
        Long codeRuntime = (currentTime - startTime) - (Long)attributes.get("frameworkRuntime");
        attributes.put("userRuntime", codeRuntime);

        this.inspectCPUDelta();
        this.inspectMemoryDelta();
    }

    /**
     * Add a custom attribute to the output.
     *
     * @param key A string to use as the key value.
     * @param value The value to associate with that key.
     */
    public void addAttribute(String key, Object value) {
        attributes.put(key, value);
    }

    /**
     * Gets a custom attribute from the attribute list.
     *
     * @param key The key of the attribute.
     * @return The object itself. Cast into appropriate data type.
     */
    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    /**
     * Add custom time stamps to the output. The key value determines the name
     * of the attribute and the value will be the time from Inspector
     * initialization to this function call.
     *
     * @param key The name of the time stamp.
     */
    public void addTimeStamp(String key) {
        Long currentTime = System.currentTimeMillis();
        attributes.put(key, currentTime - startTime);
    }

    /**
     * Add all attributes of a response object to FaaS Inspector.
     *
     * @param response The response object to consume.
     */
    public void consumeResponse(Response response) {
        Map<String, Object> responseMap = beanProperties(response);
        responseMap.keySet().forEach((s) -> {
            attributes.put(s, responseMap.get(s));
        });
    }

    /**
     * Finalize FaaS inspector. Calculator the total runtime and return the JSON
     * object containing all attributes collected.
     *
     * @return Attributes collected by FaaS Inspector.
     */
    public HashMap<String, Object> finish() {
        Long endTime = System.currentTimeMillis();
        attributes.put("runtime", endTime - startTime);
        return attributes;
    }

    /**
     * Read a file and return it as a String.
     *
     * @param filename The file name/path to read.
     * @return The entire content of the file as a string.
     */
    private static String getFileAsString(String filename) {
        File f = new File(filename);
        Path p = Paths.get(filename);
        String text;
        StringBuilder sb = new StringBuilder();
        if (f.exists()) {
            try (BufferedReader br = Files.newBufferedReader(p)) {
                while ((text = br.readLine()) != null) {
                    sb.append(text);
                    sb.append("\n");
                }
            } catch (IOException ioe) {
                sb.append("Error reading file=");
                sb.append(filename);
            }
        }
        return sb.toString();
    }

    /**
     * Execute a bash command and get the output.
     *
     * @param command An array of strings with each part of the command.
     * @return Standard out of the command.
     */
    private static String runCommand(String[] command) {
        ProcessBuilder processBuilder = new ProcessBuilder();
        processBuilder.command(command);
        try {
            Process process = processBuilder.start();
            StringBuilder output = new StringBuilder();
            BufferedReader reader = new BufferedReader(
                    new InputStreamReader(process.getInputStream()));

            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line);
                output.append("\n");
            }

            int exitVal = process.waitFor();
            if (exitVal == 0) {
                return output.toString();
            }
        } catch (IOException | InterruptedException e) {
            return "IO Exception " + e.toString();
        }
        return "ERROR";
    }

    /**
     * Convert an Object into a Map using getBeanInfo.
     * 
     * @param bean The object to collect methods from.
     * @return A HashMap representation of the object.
     * 
     * @author https://bit.ly/2ZGg4uW
     */
    private static Map<String, Object> beanProperties(Object bean) {
        try {
            return Arrays.asList(
                Introspector.getBeanInfo(bean.getClass(), Object.class)
                    .getPropertyDescriptors()
                ).stream()
                // filter out properties with setters only
                .filter(pd -> Objects.nonNull(pd.getReadMethod()))
                .collect(Collectors.toMap(
                        // bean property name
                        PropertyDescriptor::getName,
                        pd -> { // invoke method to get value
                            try {
                                return pd.getReadMethod().invoke(bean);
                            } catch (IllegalAccessException
                            | IllegalArgumentException
                            | InvocationTargetException e) {
                                // replace this with better error handling
                                return null;
                            }
                        }));
        } catch (IntrospectionException e) {
            // and this, too
            return Collections.emptyMap();
        }
    }
}
