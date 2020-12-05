# Matcher
Is a utility that compares 2 object/array and provides the comparison result in a nicely structured format. It goes up to N level, compares the 2 object/array and finds the differences. No matter shich level you have differences, it'll get it for you. 

The output format is so well defined that you can easily traverse up to N level recursively. 

You have options to pass in the ignored Attributes in a nicely nested way and so you can ignore attributes at the Nth level as well. Yes you guessed it right, you can compare the objects based on the business Key as well and you can compare Nth level objects based on the BusinessKey. If not provided either, it'll run BestMatching algo. 

Ain't it super exciting? Let's go over the result structure and examples for you to gets your hands dirty. 


## MatchingResult
```java
public class MatchingResult {
    private final MatchingStatus status;
    private final Integer matIndex;
    private final Integer elemIndex;
    private final Integer count;
    private final Map<String, MatchingResult> diff;
    private final Object exp;
    private final Object act;
    private MatchingAlgo algo;
}

public enum MatchingAlgo {
    M("MAX_COUNT"), K("BUSINESS_KEY");
}
```

## Examples 
### 1 Level Object <-> Object comparison 
```java
public class Object1 {
    private String firstName = "Raghav";
    private String secondName = "Chandra";
    private int age = 5;
}

public class Object2 {
    private String firstName = "Raghav";
    private String secondName = "Wrong";
    private String country = "India";    
}

public static void main(String[] args) {
  Object1 expected = new Object1();
  Object2 actual = new Object2();
  
  MatchingResult result = new JsonMatcher().compare(expected,actual);
}
```
Once you get the result, guess what are you going to get...Your result will have all the details you need.
```json
{
  "status": "F",
  "count": 1,
  "diff": {
    "country": {
      "status": "NW",
      "act": "India",
      "allMatching": false,
      "onlyKeyMatching": false
    },
    "firstName": {
      "status": "P",
      "algo": "M",
      "allMatching": true,
      "onlyKeyMatching": false
    },
    "age": {
      "status": "NE",
      "exp": 5,
      "allMatching": false,
      "onlyKeyMatching": false
    },
    "secondName": {
      "status": "F",
      "exp": "Chandra",
      "act": "Wrong",
      "algo": "M",
      "allMatching": false,
      "onlyKeyMatching": false
    }
  },
  "exp": {
    "firstName": "Raghav",
    "secondName": "Chandra",
    "age": 5
  },
  "act": {
    "firstName": "Raghav",
    "country": "India",
    "secondName": "Wrong"
  },
  "allMatching": false,
  "onlyKeyMatching": false
}
```

##### Based on the value in Object1 & Object2
**firstName**   : Exists in both Object1 & Object2 with matching value "Raghav" 
**secondName**  : Exists in both Object1 & Object2 but notmatching values "Chandra" vs "Wrong"
**age**         : Exists in Object1 but not in Object2 (Deleted in Object2)
**country**     : Doesn't exists in Object1 but exists in Object2 (Added in Object2)

##### Now lets look at the results
