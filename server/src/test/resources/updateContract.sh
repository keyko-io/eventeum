solc EventEmitter.sol --overwrite --bin --abi --optimize -o compiled/
web3j solidity generate -b compiled/EventEmitter.bin -a compiled/EventEmitter.abi -o ../java -p io.keyko.monitoring.agent.server.integrationtest
