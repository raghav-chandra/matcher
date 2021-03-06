# Matcher
Is a utility that compares 2 object/array and provides the comparison result in a nicely structured format. It goes up to N level, compares the 2 object/array and finds the differences. No matter shich level you have differences, it'll get it for you. 

The output format is so well defined that you can easily traverse up to N level recursively. 

You have options to pass in the ignored Attributes in a nicely nested way and so you can ignore attributes at the Nth level as well. Yes you guessed it right, you can compare the objects based on the business Key as well and you can compare Nth level objects based on the BusinessKey. If not provided either, it'll run BestMatching algo. 

To pull in maven 
```
<dependency>
    <groupId>io.github.raghav-chandra</groupId>
    <artifactId>json-matcher</artifactId>
    <version>2.0.3</version>
</dependency>
```

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
### 1 : Level 1 Object <-> Object comparison 
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
      "act": "India"
    },
    "firstName": {
      "status": "P",
      "algo": "M"
    },
    "age": {
      "status": "NE",
      "exp": 5
    },
    "secondName": {
      "status": "F",
      "exp": "Chandra",
      "act": "Wrong",
      "algo": "M"
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
  }
}
```

##### Based on the value in Object1 & Object2
```
firstName   : Exists in both Object1 & Object2 with matching value "Raghav" 
secondName  : Exists in both Object1 & Object2 but notmatching values "Chandra" vs "Wrong"
age         : Exists in Object1 but not in Object2 (Deleted in Object2)
country     : Doesn't exists in Object1 but exists in Object2 (Added in Object2)
```
##### Now lets look at the results
```
status  : 0th level status is the final status of the comparison
count   : # of matching attributes of the Object
diff    : Now since the comparison status is F - Failed, diff will be having each and every attribute  as a key and MatchingStatus (similar to top level MatchingStatus) as an object
exp     : Expected object (object1)
act     : Actual object (object2)
```
##### Now lets go more deep into diff
Since total no of attributes involve is 4 (firstName, secondName, age, country), we'll see 4 attributes in diff. Lets go over all the keys and understand what information it provides
```
firstName   : status is P means matching value in both object so no exp, act and any info required. 
secondName  : status is F means not matching values in object1/object2. This wuil have exp & act value populated to identify the difference in value.
age         : status NE means doesn't exist in Object2, exp will be populated with value from Object1.
country     : status NW means new attribute in Object2, act will be populated with value from Object2.
```

Since MatchingResult is nested under diff, those MatchingResult can have further diff in case of nested comparison and its very easy to recursively find out the differences even at the Nth level. 

### 2 : Level 1 Object <-> Object comparison (all passing)
```java
public class Object1 {
    private String firstName = "Raghav";
    private String secondName = "Chandra";
}

public class Object2 {
    private String firstName = "Raghav";
    private String secondName = "Chandra"; 
}

public static void main(String[] args) {
  Object1 expected = new Object1();
  Object2 actual = new Object2();
  
  MatchingResult result = new JsonMatcher().compare(expected,actual);
}
```
Now result will have status as P which means both of the object matched perfectly. 

### 3 : Nested level Object <-> Object comparison
```java

public class Nested1 {
    private String country = "India";
    private String city="Prayagraj";
}

public class Nested2 {
    private String country= "USA";
}

public class Object1 {
    private String name = "Raghav";
    private Nestedt1 add = new Nested1();
}

public class Object2 {
    private String name = "Raghav";
    private Nestedt2 add = new Nested2();
}

public static void main(String[] args) {
  Object1 expected = new Object1();
  Object2 actual = new Object2();
  MatchingResult result = new JsonMatcher().compare(expected,actual);
}
```
Status is clearly F as there's mismatch in the country and status. And resul looks like 
```
{
  "status": "F",
  "count": 1,
  "diff": {
    "add": {
      "status": "F",
      "diff": {
        "country": {
          "status": "F",
          "exp": "India",
          "act": "USA"
        },
        "cty": {
          "status": "NE",
          "exp": "Prayagraj"
        }
      },
      "exp": {
        "city": "Prayagraj",
        "country": "India"
      },
      "act": {
        "country": "USA"
      }
    },
    "name": {
      "status": "P"
    }
  },
  "exp": {
    "name": "Raghav",
    "add": {
      "City": "Prayagraj",
      "country": "India"
    }
  },
  "act": {
    "name": "Raghav",
    "add": {
      "country": "USA"
    }
  }
}
```
##### Now lets go more deep into diff cause Its a nested comparison and will have nested diff 
```
level 1 ->
status  : F, overall not matching
act     : Object2
exp     : Objec1 
diff    : name has status P means value is matching. add has status F means not matching. It has exp (nested1 object) and act object (nested2 object).

level 2 -> Difference of add
status  : F means, not matching
diff    : country status is F means country value not matching and has act, exp which contains 2 different values from 2 object. city has status as NE means it doesn't exist in nested2 object.  
```

### 4 : Primitive comparison
```java
public static void main(String[] args) {
  MatchingResult result = new JsonMatcher().compare(3, 4);
}
```
Result is clear that its failure 3 vs 4 values
```
{
  "status": "F",
  "count": 0,
  "exp": 3,
  "act": 4
}
```
