javac -cp .:lib/* -d bin src/bot/*.java src/com/*.java src/controllers/lithoRasters/*.java src/controllers/*.java src/gds/*.java src/gui/*.java src/main/*.java
cd bin
javah com.MatrixInterface
cd ..
cp bin/com_MatrixInterface.h src/com_MatrixInterface.h
cp bin/com_MatrixInterface.h ../ABDController_C_Code/vc/com_MatrixInterface/com_MatrixInterface/com_MatrixInterface.h
