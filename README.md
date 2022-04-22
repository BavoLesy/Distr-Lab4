# Distr-Lab4 commands
git reset --hard
git pull https://github.com/BavoLesy/Distr-Lab4.git
mvn package
mvn spring-boot:run
mvn exec:java -Dexec.mainClass="Node.NamingNode" -Dexec.args="jefke"
mvn exec:java -Dexec.mainClass="Node.NamingNode" -Dexec.args="bavo"
mvn exec:java -Dexec.mainClass="Node.NamingNode" -Dexec.args="oliver"
mvn exec:java -Dexec.mainClass="Node.NamingNode" -Dexec.args="max"
cd distributed 
cd Distr-Lab4
cd
cd ..
git clone https://github.com/BavoLesy/Distr-Lab4.git
rm -rf Distr-Lab4
