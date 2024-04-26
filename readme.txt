This is a project I made for CPSC 501. (Advanced Programming Techniques)
The purpose of the assignment was to create an object serializer/deserializer using reflective programming in Java. In order to test my code, I wrote a primitive REPL.

To run the project, use the following command:
    java -jar Executable.jar

When running the program, you can use 'help' to get a list of commands.

The program is meant to be used as a REPL. (Though the parsing is quite primitive; instead of proper tokenization and AST construction, it just uses string-split instead. Thus, nested comma-separated lists will not parse correctly.)

The user can run the program twice at once on a single computer, and configure the two instances to communicate. If you've worked with the socket library before, usage should be relatively intutive.

Keep in mind that this project was primarily designed for my own use. Apologies for the lack of documentation!

Sample usage: (the 2 programs below run in parallel, and communicate with each other)


PROGRAM 1:
-------------------------------------------------------------------------------------------------------------
> sock
Made a server socket. Try any of the below addresses:
     127.0.0.1:59862
     127.0.0.1:59862
     localhost:59862

No client. Use 'acc' to accept connection.
> acc
waiting for connection...
Connection established!
> b = new Linked(4)
> a = new Linked(3,b)
> a
3 -> 4 -> null
> b.next = a
> a
3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> ...
> send a
<?xml version="1.0" encoding="UTF-8"?>
<serialized>
  <object class="Linked" id="0">
    <field name="next" declaringclass="Linked">
      <reference>1</reference>
    </field>
    <field name="val" declaringclass="Linked">
      <value>3</value>
    </field>
  </object>
  <object class="Linked" id="1">
    <field name="next" declaringclass="Linked">
      <reference>0</reference>
    </field>
    <field name="val" declaringclass="Linked">
      <value>4</value>
    </field>
  </object>
</serialized>


> exit
done

PROGRAM 2:
-------------------------------------------------------------------------------------------------------------
> conn 127.0.0.1:59862                                                                                                  Trying to connect...                                                                                                    Connection established!                                                                                                 > recv a                                                                                                                3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> ...                 > a.next                                                                                                                4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> 4 -> 3 -> ...                 > exit                                                                                                                  done