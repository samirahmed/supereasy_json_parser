import scala.annotation.migration
import scala.util.parsing.combinator.JavaTokenParsers

/*
 * Created by Samir Ahmed
 * http://www.samir-ahmed.com
 * https://www.github.com/samirahmed
 * 
 * APACHE-2.0 LICENSE
 * Copyright 2011 Samir Ahmed

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
 *
 */

/**
 * Slight Adapted version of Escaped String Parser
 * to enable more complete string parsing
 * Taken from com.twitter.json at https://github.com/stevej/scala-json
 */
class EscapedStringParser extends JavaTokenParsers {
	  override protected val whiteSpace = "".r
	
	  def unicode: Parser[String] = rep1("\\u" ~> """[a-fA-F0-9]{4}""".r) ^^ { stringBytes =>
	    new String(stringBytes.map(Integer.valueOf(_, 16).intValue.asInstanceOf[Char]).toArray)
	  }
	
	  def escaped: Parser[String] = "\\" ~> """[\\/bfnrt"]""".r ^^ { charStr =>
	    val char = charStr match {
	      case "r" => '\r'
	      case "n" => '\n'
	      case "t" => '\t'
	      case "b" => '\b'
	      case "f" => '\f'
	      case x => x.charAt(0)
	    }
	    char.toString
	  }
	
	  def characters: Parser[String] = """[^\"[\x00-\x1F]\\]+""".r
	
	  def string: Parser[String] = "\"" ~> rep(unicode | escaped | characters) <~ "\"" ^^ { list =>
	    list.mkString("")
	  }
	
	  def parse(s: String) = {
	    parseAll(string, s)
	    }
  }

/**
 * SuperEasy JSON Parser for 
 * JSON Parser Adapted from Martin Odersky's Programming in Scala and SteveJ's Twitter Scala-JSON
 * 
 * First Parse, then get
 * */
class supereasy_json_parser extends JavaTokenParsers {
	
	// Private Data member for collecting all JSON Members
	private var memberListing = List[(String,Any)]()
	private var isParsed = false
	lazy val stringParser = (new EscapedStringParser)
	
	// Define a valid value as a JSON object, JSON array or JSON member
	def value: Parser[Any] = (
	  obj
	  |arr
	  |member
	  |string
	  |floatingPointNumber
  	  |"null" ^^ (x => null)
  	  |"true" ^^ (x => true) 
	  |"false" ^^ (x => false)
	)
	
	// Parse everything between {} as members separated by commas
	def obj: Parser[Map[String, Any]] = "{"~>repsep(member, ",")<~"}" ^^ (Map() ++ _) 
 
	// Parse all JSON arrays as lists
    def arr: Parser[List[Any]] = "["~>repsep(value, ",")<~"]" 

    // Parse all name value pairs into tuples, collect them in the memberListing
    def member: Parser[(String, Any)] = stringLiteral~":"~value ^^ {  case name~":"~value => 
      { memberListing ::= (name,value); (name,value) }  }
    
	def string: Parser[String] = "\"(\\\\\\\\|\\\\\"|[^\"])*+\"".r ^^ 
	{ escapedStr => stringParser.parse(escapedStr).get }
	    
    // Override original parseAll to reverse List 
    def parse (rawJSON : String ): Any = {
      val parseResult = super.parseAll(value,rawJSON)
      memberListing = memberListing.reverse
      isParsed = true
      parseResult.get
    }
      
    /** 
     * Returns a string containing the value of nth instance of a JSON member (in order of appearence)
     * when given the member's identifier. Even if the member is nested within JSON Arrays or Objects
     * 
     * E.g Raw JSON String {"State":"Florida","Place":{"State":"Wisconsin"}}
     * 'Wisconsin' can be extracted by calling getValue("State",2)
     * Will throw NoSuchFieldException in the event a non primitive JSON value is requested (JSON Object or Array)
     */
    def getValue( memberName: String, instance :Int = 1 ): String = {
      get(memberName, instance) match {
        case value:List[Any] => invalidRequest(memberName,value.toString() )
        case value:Map[String,Any] => invalidRequest(memberName,value.toString() )
        case value => value.toString;
      }
    }
    
    /** 
     * Returns a List containing a JSON Array that matches the given memberName's nth instance (in order of appearence)
     * 
     * E.g Raw JSON String {"State":["NY" "MA"],"State",["FL" "CA"]}
     * List("FL","CA") can be extracted by calling getArray("State",2)
     * Will throw NoSuchFieldException in the event a non primitive JSON Array is requested (JSON Object or MemberValue)
     */
    def getArray( memberName: String, instance :Int = 1): List[Any] = {
      get(memberName, instance) match {
        case value:List[Any] => value
        case value:Map[String,Any] => invalidRequest(memberName,value.toString() )
        case value => invalidRequest(memberName,value.toString())
      }
    }

    /** 
     * Returns a Map[String, Any] containing a JSON Object that matches the given memberName's nth instance (in order of appearence)
     * 
     * E.g Raw JSON String {"State":{"Capital":"Boston"},"State":{"Capital":"Albany"}}
     * Map("Capital","Albany") can be extracted by calling getObject("State",2)
     * Will throw NoSuchFieldException in the event a non primitive JSON Array is requested (JSON Object or MemberValue)
     */
    def getObject( memberName: String, instance :Int = 1): Map[String,Any] = {
      get(memberName, instance) match {
        case value:List[Any] => invalidRequest(memberName,value.toString())
        case value:Map[String,Any] => value
        case value => invalidRequest(memberName,value.toString() )
      }
    }
    
    /**
     * Returns all non JSON Object or Array, member values that match the given MemberName
     * 
     * e.g Raw JSON String {"State":{"Capital":"Boston"},"State":{"Capital":"Albany"}}
     * Calling 'getAllValues("Capital")' will return List("Boston"."Albany")
     */
    def getAllValues(memberName: String):List[String] = {
      get(memberName,-1) match{
        case values: List[(String,Any)] => for (value <- values; if isValue(value._2) ) yield value._2.toString()
            }
      }
    
    def isValue ( jsonValue: Any): Boolean = {
      jsonValue match{
        case value : List[Any] => false 
        case value : Map[String,Any] => false
        case value => true
      }
    }
    
    /**
     * Returns a List with every JSON member name and value (Objects and Arrays included)
     */
    def getFullMemberListing(): List[(String,Any)]={
      return this.memberListing;
    }
    
    /* Private Helper Functions */
    
    // If not yet Parsed, we throw an exception
    private def checkIfParsed() = {
      if (!isParsed){
      throw new Exception("No JSON String has been parsed, Try using the parseAll method first")
      }
    }
    
    private def get(memberName: String, instance: Int): Any = {
      
      def ensureQuotationMarks(): String = {
	    if (!memberName.matches("\".*?\"")){
	        return '"'+memberName+'"'
	    	}          
	    memberName
		}
	      
      checkIfParsed()
      val name = ensureQuotationMarks()
      val valid = memberListing.filter( _._1.equals(name))
      
      // If instance equals -1, return all of instances
      if (instance == -1){
        return valid
      }
      
      if (valid.length < instance){
      throw new NoSuchFieldException("Could not find instance number " +instance+ " of member with name: "+memberName)
      }
      valid(instance-1)._2
    }
    
    private def invalidRequest(name :String, value:String)={
      throw new NoSuchFieldException(name+" does not yield requested JSON Type, but instead "+value)
    }
    
}

object supereasyTester extends supereasy_json_parser{
  
  def main (args : Array[String]): Unit = {
	
    // Load test file and parse
    val rawJSON = io.Source.fromFile("sampleJSON.txt").mkString;
    val start = System.currentTimeMillis();
    parse(rawJSON)
    
    // Test the various methods
    println ( "FirstURL : "+getValue("FirstURL") )
    println ( "\nFirstURL : "+getValue("\"FirstURL\"") )
    println ( "\nSecond Instance of FirstURL : "+getValue("FirstURL",2) )
    println ( "\nThird Instance of Topics Array : \n"+getArray("Topics",3).mkString("\n") )
    println ( "\nFourth Instance of Icon Object : "+getObject("Icon",4) )
    println ( "\nAll Non JSON Object, Array Values \n: "+getAllValues("Text").mkString("\n") ) 
    println ( "\nFull Member List : \n"+getFullMemberListing().mkString("\n"))
    println ("Total Time: "+(System.currentTimeMillis()-start))
  }
}