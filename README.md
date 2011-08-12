[scala-json]: https://github.com/stevej/scala-json/blob/master/src/main/scala/com/twitter/json/Json.scala
[Steve Jenson]: https://www.github.com/stevej
[Programming in Scala]: http://www.amazon.com/Programming-Scala-Comprehensive-Step-Step/dp/0981531644
[Martin Odersky's]: http://lamp.epfl.ch/~odersky/

# README:  Super Easy JSON Parser

Designed to be the most simple way to extract data from JSON, the Super Easy JSON Parser is 
adapted from [Martin Odersky's][Programming in Scala] book and the more robust string parsing
from [scala-json] by [Steve Jenson].

## Super Easy Methods and examples

#### Sample JSON File - rawjson.txt

	{
	     "firstName": "John",
	     "lastName": "Smith",
	     "age": 25,
		 "hometown":["Brooklyn", "New York"],
	     "address":
	     {
	         "streetAddress": "21 2nd Street",
	         "city": "New York",
	         "state": "NY",
	         "postalCode": "10021"
	     },
	     "phoneNumber":
	     [
	         {
	           "type": "home",
	           "number": "212 555-1234"
	         },
	         {
	           "type": "fax",
	           "number": "646 555-4567"
	         }
	     ]
	 }

#### Sample Usage

After calling the parse(rawjson :String) method

	getValue("type") 			returns "home" 
	getValue("\"type\"")		returns	"home"
	getValue("type",2) 			returns "fax"
	getAllValues("type",2) 		returns List("home","fax")
	getObject("address")		returns Map("streetAddress"->"21 2nd Street","city"->"New York","State"->"NY","postalCode"->"10021")
	getArray("hometown")		returns List("Brooklyn", "New York")

## Licensing

   Copyright 2011 Samir Ahmed
   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.

