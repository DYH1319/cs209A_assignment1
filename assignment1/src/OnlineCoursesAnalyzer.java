import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;
import java.util.stream.Collectors;

public class OnlineCoursesAnalyzer {
    
    List<OnlineCourse> courses;
    
    public OnlineCoursesAnalyzer(String datasetPath) {
        try {
            courses = Files.lines(Paths.get(datasetPath), StandardCharsets.UTF_8)
                    .skip(1)
                    .map(l -> l.split(",(?=\\S)"))
                    .map(a -> new OnlineCourse(a[0], a[1],
                            a[2].substring(6) + "/" + a[2].substring(0, 5),
                            a[3].replace("\"", ""), a[4].replace("\"", ""),
                            a[5].replace("\"", ""), Integer.parseInt(a[6]), Integer.parseInt(a[7]),
                            Integer.parseInt(a[8]), Integer.parseInt(a[9]), Integer.parseInt(a[10]),
                            Double.parseDouble(a[11]), Double.parseDouble(a[12]), Double.parseDouble(a[13]),
                            Double.parseDouble(a[14]), Double.parseDouble(a[15]), Double.parseDouble(a[16]),
                            Double.parseDouble(a[17]), Double.parseDouble(a[18]), Double.parseDouble(a[19]),
                            Double.parseDouble(a[20]), Double.parseDouble(a[21]), Double.parseDouble(a[22])))
                    .toList();
        } catch (Exception ignore) {
        
        }
    }
    
    public Map<String, Integer> getPtcpCountByInst() {
        return courses.stream()
                .sorted(((o1, o2) -> Objects.compare(o2.getInstitution(), o1.getInstitution(), String::compareTo)))
                .collect(Collectors.groupingBy(OnlineCourse::getInstitution,
                        Collectors.summingInt(OnlineCourse::getParticipants)));
    }
    
    public Map<String, Integer> getPtcpCountByInstAndSubject() {
        return courses.stream()
                .collect(Collectors.groupingBy(e -> e.getInstitution() + "-" + e.getCourseSubject(),
                        Collectors.summingInt(OnlineCourse::getParticipants)))
                .entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue().reversed()
                        .thenComparing(Map.Entry.comparingByKey()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, Integer::sum, LinkedHashMap::new));
    }
    
    public Map<String, List<List<String>>> getCourseListOfInstructor() {
        Map<String, List<Set<String>>> result = new HashMap<>();
        
        for (OnlineCourse course : courses) {
            String getInstructors = course.getInstructors();
            String courseName = course.getCourseTitle();
            String[] instructors = getInstructors.split(", ");
            
            if (instructors.length == 1) {
                if (result.containsKey(instructors[0])) {
                    result.get(instructors[0]).get(0).add(courseName);
                } else {
                    result.put(instructors[0], new ArrayList<>());
                    result.get(instructors[0]).add(new HashSet<>());
                    result.get(instructors[0]).add(new HashSet<>());
                    result.get(instructors[0]).get(0).add(courseName);
                }
            } else {
                for (String instructor : instructors) {
                    if (result.containsKey(instructor)) {
                        result.get(instructor).get(1).add(courseName);
                    } else {
                        result.put(instructor, new ArrayList<>());
                        result.get(instructor).add(new HashSet<>());
                        result.get(instructor).add(new HashSet<>());
                        result.get(instructor).get(1).add(courseName);
                    }
                }
            }
        }
        
        Map<String, List<List<String>>> result2 = new HashMap<>();
        result.forEach((k, v) -> {
            result2.put(k, new ArrayList<>());
            result2.get(k).add(new ArrayList<>());
            result2.get(k).add(new ArrayList<>());
            result2.get(k).get(0).addAll(v.get(0).stream().toList());
            result2.get(k).get(1).addAll(v.get(1).stream().toList());
        });
        
        result2.forEach((k, v) -> {
            v.get(0).sort(String::compareTo);
            v.get(1).sort(String::compareTo);
        });
        
        return result2;
    }
    
    public List<String> getCourses(int topK, String by) {
        if (by.equals("hours")) {
            return courses.stream()
                    .sorted(Comparator.comparingDouble(OnlineCourse::getTotalCourseHours).reversed()
                            .thenComparing(OnlineCourse::getCourseTitle))
                    .map(OnlineCourse::getCourseTitle)
                    .distinct()
                    .limit(topK)
                    .collect(Collectors.toList());
        } else if (by.equals("participants")) {
            return courses.stream()
                    .sorted(Comparator.comparingInt(OnlineCourse::getParticipants).reversed()
                            .thenComparing(OnlineCourse::getCourseTitle))
                    .map(OnlineCourse::getCourseTitle)
                    .distinct()
                    .limit(topK)
                    .collect(Collectors.toList());
        }
        return null;
    }
    
    public List<String> searchCourses(String courseSubject, double percentAudited, double totalCourseHours) {
        return courses.stream()
                .filter(e -> e.get_audited() >= percentAudited &&
                             e.getTotalCourseHours() <= totalCourseHours &&
                             e.getCourseSubject().toLowerCase().contains(courseSubject.toLowerCase()))
                .sorted(Comparator.comparing(OnlineCourse::getCourseTitle))
                .map(OnlineCourse::getCourseTitle)
                .distinct()
                .collect(Collectors.toList());
    }
    
    public List<String> recommendCourses(int age, int gender, int isBachelorOrHigher) {
        Map<String, Double> averageMedianAge = courses.stream()
                .collect(Collectors.groupingBy(OnlineCourse::getCourseNumber))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .mapToDouble(OnlineCourse::getMedianAge)
                                .average()
                                .orElseThrow()));
        Map<String, Double> average_male = courses.stream()
                .collect(Collectors.groupingBy(OnlineCourse::getCourseNumber))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .mapToDouble(OnlineCourse::get_male)
                                .average()
                                .orElseThrow()));
        Map<String, Double> average_bachelor_sDegreeOrHigher = courses.stream()
                .collect(Collectors.groupingBy(OnlineCourse::getCourseNumber))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .mapToDouble(OnlineCourse::get_bachelor_sDegreeOrHigher)
                                .average()
                                .orElseThrow()));
        Map<String, String> courseNumber_courseTitle = courses.stream()
                .collect(Collectors.groupingBy(OnlineCourse::getCourseNumber))
                .entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey,
                        e -> e.getValue().stream()
                                .sorted(Comparator.comparing(OnlineCourse::getLaunchDate).reversed())
                                .map(OnlineCourse::getCourseTitle)
                                .findFirst()
                                .orElseThrow()));
        Map<String, Double> similarityValue = new LinkedHashMap<>();
        averageMedianAge.forEach((k, v) ->
                similarityValue.put(k, Math.pow(age - v, 2) +
                                       Math.pow(gender * 100 - average_male.get(k), 2) +
                                       Math.pow(isBachelorOrHigher * 100 - average_bachelor_sDegreeOrHigher.get(k), 2)));
        
        similarityValue.entrySet().stream().sorted(Map.Entry.comparingByValue())
                .forEach(e -> System.out.println(e.getKey() + "==" + e.getValue()));
        
        Map<String, Double> similarityValue2 = new HashMap<>();
        similarityValue.forEach((k, v) -> similarityValue2.put(courseNumber_courseTitle.get(k), v));
        
        return similarityValue2.entrySet().stream()
                .sorted(Map.Entry.<String, Double>comparingByValue()
                        .thenComparing(Map.Entry.comparingByKey()))
                .limit(10)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }
}

class OnlineCourse {
    public String institution;
    public String courseNumber;
    public String launchDate;
    public String courseTitle;
    public String instructors;
    public String courseSubject;
    public int year;
    public int honorCodeCertificates;
    public int participants;
    public int audited;
    public int certified;
    public double _audited;
    public double _certified;
    public double _certifiedOfLargerThanFiftyPercentCourseContentAccessed;
    public double _playedVideo;
    public double _postedInForum;
    public double _gradeHigherThanZero;
    public double totalCourseHours;
    public double medianHoursForCertification;
    public double medianAge;
    public double _male;
    public double _female;
    public double _bachelor_sDegreeOrHigher;
    
    public OnlineCourse(String institution, String courseNumber, String launchDate, String courseTitle, String instructors, String courseSubject, int year, int honorCodeCertificates, int participants, int audited, int certified, double _audited, double _certified, double _certifiedOfLargerThanFiftyPercentCourseContentAccessed, double _playedVideo, double _postedInForum, double _gradeHigherThanZero, double totalCourseHours, double medianHoursForCertification, double medianAge, double _male, double _female, double _bachelor_sDegreeOrHigher) {
        this.institution = institution;
        this.courseNumber = courseNumber;
        this.launchDate = launchDate;
        this.courseTitle = courseTitle;
        this.instructors = instructors;
        this.courseSubject = courseSubject;
        this.year = year;
        this.honorCodeCertificates = honorCodeCertificates;
        this.participants = participants;
        this.audited = audited;
        this.certified = certified;
        this._audited = _audited;
        this._certified = _certified;
        this._certifiedOfLargerThanFiftyPercentCourseContentAccessed = _certifiedOfLargerThanFiftyPercentCourseContentAccessed;
        this._playedVideo = _playedVideo;
        this._postedInForum = _postedInForum;
        this._gradeHigherThanZero = _gradeHigherThanZero;
        this.totalCourseHours = totalCourseHours;
        this.medianHoursForCertification = medianHoursForCertification;
        this.medianAge = medianAge;
        this._male = _male;
        this._female = _female;
        this._bachelor_sDegreeOrHigher = _bachelor_sDegreeOrHigher;
    }
    
    public String getInstitution() {
        return institution;
    }
    
    public String getCourseNumber() {
        return courseNumber;
    }
    
    public String getLaunchDate() {
        return launchDate;
    }
    
    public String getCourseTitle() {
        return courseTitle;
    }
    
    public String getInstructors() {
        return instructors;
    }
    
    public String getCourseSubject() {
        return courseSubject;
    }
    
    public int getYear() {
        return year;
    }
    
    public int getHonorCodeCertificates() {
        return honorCodeCertificates;
    }
    
    public int getParticipants() {
        return participants;
    }
    
    public int getAudited() {
        return audited;
    }
    
    public int getCertified() {
        return certified;
    }
    
    public double get_audited() {
        return _audited;
    }
    
    public double get_certified() {
        return _certified;
    }
    
    public double get_certifiedOfLargerThanFiftyPercentCourseContentAccessed() {
        return _certifiedOfLargerThanFiftyPercentCourseContentAccessed;
    }
    
    public double get_playedVideo() {
        return _playedVideo;
    }
    
    public double get_postedInForum() {
        return _postedInForum;
    }
    
    public double get_gradeHigherThanZero() {
        return _gradeHigherThanZero;
    }
    
    public double getTotalCourseHours() {
        return totalCourseHours;
    }
    
    public double getMedianHoursForCertification() {
        return medianHoursForCertification;
    }
    
    public double getMedianAge() {
        return medianAge;
    }
    
    public double get_male() {
        return _male;
    }
    
    public double get_female() {
        return _female;
    }
    
    public double get_bachelor_sDegreeOrHigher() {
        return _bachelor_sDegreeOrHigher;
    }
}