### Confluence River for Elasticsearch

Welcome to the Confluence River Plugin for "Elasticsearch": http://www.elasticsearch.org

### Versions

|_. RSS River Plugin|_. ElasticSearch|
|  master (0.0.1)   |  master (0.19) |

### Getting Started

#### Installation

Just type :

<pre>
$ bin\plugin -install obazoud/elasticsearch-river-confluence/0.0.1
</pre>

<pre>
-> Installing obazoud/elasticsearch-river-confluence/0.0.1...
Trying https://github.com/downloads/obazoud/elasticsearch-river-confluence-0.0.1.zip...
Downloading ...DONE
Installed confluence-river
</pre>

#### Creating a Confluence river

We create first an index to store all the pages

<pre>
$ curl -XPUT 'localhost:9200/confluence/' -d '{}'
</pre>

We create the river with the following properties :

* Confluence URL : http://confluence.mycompany.com
* Update Rate : every 5 minutes (5 * 60 * 1000 = 300000 ms)
* User: <username>,
* Password: <password>"
* spaceKey: "mySpace"

<pre>
$ curl -XPUT 'localhost:9200/_river/confluence/_meta' -d '{
  "type": "confluence",
  "confluence": {
   "url": "http://confluence.fullsix.com",
   "username": "<username>",
   "password": "<password>",
   "spaceKey": "<confluence space key>"
  }
}'
</pre>


### To Do List

Many many things to do :
TBD


