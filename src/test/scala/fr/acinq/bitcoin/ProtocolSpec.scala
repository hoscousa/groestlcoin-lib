package fr.acinq.bitcoin

import java.math.BigInteger
import java.net.InetAddress

import com.google.common.io.ByteStreams
import org.junit.runner.RunWith
import org.scalatest.FlatSpec
import org.scalatest.junit.JUnitRunner

@RunWith(classOf[JUnitRunner])
class ProtocolSpec extends FlatSpec {
  "Protocol" should "parse blochain blocks" in {
    val stream = classOf[ProtocolSpec].getResourceAsStream("/block1.dat")
    val block = Block.read(stream)
    assert(Block.checkProofOfWork(block))
    // check that we can deserialize and re-serialize scripts
    block.tx.map(tx => {
      tx.txIn.map(txin => {
        if (!OutPoint.isCoinbase(txin.outPoint)) {
          val script = Script.parse(txin.signatureScript)
          assert(txin.signatureScript == Script.write(script))
        }
      })
      tx.txOut.map(txout => {
        val script = Script.parse(txout.publicKeyScript)
        assert(txout.publicKeyScript == Script.write(script))
      })
    })
  }
  it should "serialize/deserialize blocks" in {
    val stream = classOf[ProtocolSpec].getResourceAsStream("/block1.dat")
    val bytes: BinaryData = ByteStreams.toByteArray(stream)
    val block = Block.read(bytes)
    val check = Block.write(block)
    assert(check == bytes)
  }
  it should "decode transactions" in {
    // data copied from https://people.xiph.org/~greg/signdemo.txt
    val tx = Transaction.read("01000000010c432f4fb3e871a8bda638350b3d5c698cf431db8d6031b53e3fb5159e59d4a90000000000ffffffff0100f2052a010000001976a9143744841e13b90b4aca16fe793a7f88da3a23cc7188ac00000000")
    val script = Script.parse(tx.txOut(0).publicKeyScript)
    val publicKeyHash = Script.publicKeyHash(script)
    assert(Base58Check.encode(Base58.Prefix.PubkeyAddressTestnet, publicKeyHash) === "mkZBYBiq6DNoQEKakpMJegyDbw2YiNQnHT")
  }
  it should "generate genesis block" in {
    assert(Block.write(Block.LivenetGenesisBlock) === BinaryData("700000000000000000000000000000000000000000000000000000000000000000000000bb2866aaca46c4428ad08b57bc9d1493abaf64724b6c3052a7c8f958df68e93ced3d2b53ffff0f1e835b03000101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff3a04ffff001d0104325072657373757265206d75737420626520707574206f6e20566c6164696d697220507574696e206f766572204372696d6561ffffffff010000000000000000434104678afdb0fe5548271967f1a67130b7105cd6a828e03909a67962e0ea1f61deb649f6bc3f4cef38c4f35504e51ec112de5c384df7ba0b8d578a4c702b6bf11d5fac00000000"))
    assert(Block.LivenetGenesisBlock.blockId === BinaryData("00000ac5927c594d49cc0bdb81759d0da8297eb614683d3acb62f0703b639023"))
    assert(Block.TestnetGenesisBlock.blockId === BinaryData("000000ffbb50fc9898cdd36ec163e6ba23230164c0052a28876255b7dcf2cd36"))
    assert(Block.RegtestGenesisBlock.blockId === BinaryData("0f9188f13cb7b2c71f2a335e3a4fc328bf5beb436012afca590b1a11466e2206"))
    assert(Block.SegnetGenesisBlock.blockId === BinaryData("18fb5ff510c09532033d2137a6914010509ee6258275a4b7e1b7b24b1d2191b2"))
  }
  it should "decode proof-of-work difficulty" in {
    assert(decodeCompact(0) === (BigInteger.ZERO, false, false))
    assert(decodeCompact(0x00123456) === (BigInteger.ZERO, false, false))
    assert(decodeCompact(0x01003456) === (BigInteger.ZERO, false, false))
    assert(decodeCompact(0x02000056) === (BigInteger.ZERO, false, false))
    assert(decodeCompact(0x03000000) === (BigInteger.ZERO, false, false))
    assert(decodeCompact(0x04000000) === (BigInteger.ZERO, false, false))
    assert(decodeCompact(0x00923456) === (BigInteger.ZERO, false, false))
    assert(decodeCompact(0x01803456) === (BigInteger.ZERO, false, false))
    assert(decodeCompact(0x02800056) === (BigInteger.ZERO, false, false))
    assert(decodeCompact(0x03800000) === (BigInteger.ZERO, false, false))
    assert(decodeCompact(0x04800000) === (BigInteger.ZERO, false, false))
    assert(decodeCompact(0x01123456) === (BigInteger.valueOf(0x12), false, false))
    assert(decodeCompact(0x01fedcba) === (BigInteger.valueOf(0x7e), true, false))
    assert(decodeCompact(0x02123456) === (BigInteger.valueOf(0x1234), false, false))
    assert(decodeCompact(0x03123456) === (BigInteger.valueOf(0x123456), false, false))
    assert(decodeCompact(0x04123456) === (BigInteger.valueOf(0x12345600), false, false))
    assert(decodeCompact(0x04923456) === (BigInteger.valueOf(0x12345600), true, false))
    assert(decodeCompact(0x05009234) === (new BigInteger(1, BinaryData("92340000")), false, false))
    assert(decodeCompact(0x20123456) === (new BigInteger(1, BinaryData("1234560000000000000000000000000000000000000000000000000000000000")), false, false))
    val (_, false, true) = decodeCompact(0xff123456L)
  }
  it should "read and write version messages" in {
    val version = Version(
      0x00011172L,
      services = 1L,
      timestamp = 0x53c420c4L,
      addr_recv = NetworkAddress(1L, InetAddress.getByAddress(Array(85.toByte, 235.toByte, 17.toByte, 3.toByte)), 18333L),
      addr_from = NetworkAddress(1L, InetAddress.getByAddress(Array(109.toByte, 24.toByte, 186.toByte, 185.toByte)), 18333L),
      nonce = 0x4317be39ae6ea291L,
      user_agent = "/Satoshi:0.9.99/",
      start_height = 0x00041a23L,
      relay = true)

    assert(Version.write(version) === BinaryData("721101000100000000000000c420c45300000000010000000000000000000000000000000000ffff55eb1103479d010000000000000000000000000000000000ffff6d18bab9479d91a26eae39be1743102f5361746f7368693a302e392e39392f231a040001"))

    val message = Message(magic = 0x0709110bL, command = "version", payload = Version.write(version))
    assert(Message.write(message) === BinaryData("0b11090776657273696f6e0000000000660000008c48bb56721101000100000000000000c420c45300000000010000000000000000000000000000000000ffff55eb1103479d010000000000000000000000000000000000ffff6d18bab9479d91a26eae39be1743102f5361746f7368693a302e392e39392f231a040001"))

    val message1 = Message.read(Message.write(message))
    assert(message1.command === "version")
    val version1 = Version.read(message1.payload)
    assert(version1 === version)
  }
  it should "read and write verack messages" in {
    val message = Message.read("0b11090776657261636b000000000000000000005df6e0e2")
    assert(message.command === "verack")
    assert(message.payload.data.isEmpty)

    val message1 = Message(magic = 0x0709110bL, command = "verack", payload = Array.empty[Byte])
    assert(Message.write(message1) === BinaryData("0b11090776657261636b000000000000000000005df6e0e2"))
  }
  it should "read and write addr messages" in {
    // example take from https://en.bitcoin.it/wiki/Protocol_specification#addr
    val message = Message.read("f9beb4d96164647200000000000000001f000000ed52399b01e215104d010000000000000000000000000000000000ffff0a000001208d")
    assert(message.command === "addr")
    val addr = Addr.read(message.payload)
    assert(addr.addresses.length === 1)
    assert(addr.addresses(0).address.getAddress === Array(10: Byte, 0: Byte, 0: Byte, 1: Byte))
    assert(addr.addresses(0).port === 8333)

    val addr1 = Addr(List(NetworkAddressWithTimestamp(time = 1292899810L, services = 1L, address = InetAddress.getByAddress(Array(10: Byte, 0: Byte, 0: Byte, 1: Byte)), port = 8333)))
    val message1 = Message(magic = 0xd9b4bef9, command = "addr", payload = Addr.write(addr1))
    assert(Message.write(message1) === BinaryData("f9beb4d96164647200000000000000001f000000ed52399b01e215104d010000000000000000000000000000000000ffff0a000001208d"))
  }
  it should "read and write addr messages 2" in {
    val stream = classOf[ProtocolSpec].getResourceAsStream("/addr.dat")
    val message = Message.read(stream)
    assert(message.command === "addr")
    val addr = Addr.read(message.payload)
    assert(addr.addresses.length === 1000)
  }
  it should "read and write inventory messages" in {
    val inventory = Inventory.read("01010000004d43a12ddedc1638542a4c5a5dff3fc5daa9bd543ecccbe8c7eed8648044668f")
    assert(inventory.inventory.length === 1)
    assert(inventory.inventory(0).`type` === InventoryVector.MSG_TX)
  }
  it should "read and write inventory messages 2" in {
    val stream = classOf[ProtocolSpec].getResourceAsStream("/inv.dat")
    val message = Message.read(stream)
    assert(message.command === "inv")
    val inv = Inventory.read(message.payload)
    assert(inv.inventory.size === 500)
    assert(message.payload == BinaryData(Inventory.write(inv)))
  }
  it should "read and write getblocks messages" in {
    val message = Message.read("f9beb4d9676574626c6f636b7300000045000000f5fcbcad72110100016fe28c0ab6f1b372c1a6a246ae63f74f931e8365e15a089c68d61900000000000000000000000000000000000000000000000000000000000000000000000000")
    assert(message.command == "getblocks")
    val getblocks = Getblocks.read(message.payload)
    assert(getblocks.version === 70002)
    assert(getblocks.locatorHashes(0).toString === "6fe28c0ab6f1b372c1a6a246ae63f74f931e8365e15a089c68d6190000000000")
    assert(Getblocks.write(getblocks) === message.payload)
  }
  it should "read and write getheaders message" in {
    val getheaders = Getheaders.read("711101000106226e46111a0b59caaf126043eb5bbf28c34f3a5e332a1fc7b2b73cf188910f0000000000000000000000000000000000000000000000000000000000000000")
    assert(getheaders.locatorHashes(0) === Block.RegtestGenesisBlock.hash)
    assert(Getheaders.write(getheaders) === BinaryData("711101000106226e46111a0b59caaf126043eb5bbf28c34f3a5e332a1fc7b2b73cf188910f0000000000000000000000000000000000000000000000000000000000000000"))
  }
  it should "read and write getdata messages" in {
    val stream = classOf[ProtocolSpec].getResourceAsStream("/getdata.dat")
    val message = Message.read(stream)
    assert(message.command === "getdata")
    val getdata = Getdata.read(message.payload)
    assert(getdata.inventory.size === 128)
    assert(getdata.inventory(0).hash === BinaryData("4860eb18bf1b1620e37e9490fc8a427514416fd75159ab86688e9a8300000000"))
    val check = Getdata.write(getdata)
    assert(BinaryData(check) == message.payload)
  }
  it should "read and write block messages" in {
    val message = Message.read("f9beb4d9626c6f636b00000000000000d7000000934d270a010000006fe28c0ab6f1b372c1a6a246ae63f74f931e8365e15a089c68d6190000000000982051fd1e4ba744bbbe680e1fee14677ba1a3c3540bf7b1cdb606e857233e0e61bc6649ffff001d01e362990101000000010000000000000000000000000000000000000000000000000000000000000000ffffffff0704ffff001d0104ffffffff0100f2052a0100000043410496b538e853519c726a2c91e61ec11600ae1390813a627c66fb8be7947be63c52da7589379515d4e0a604f8141781e62294721166bf621e73a82cbf2342c858eeac00000000")
    assert(message.command === "block")
    val block = Block.read(message.payload)
    assert(block.header.hashPreviousBlock == Block.LivenetGenesisBlock.hash)
    assert(OutPoint.isCoinbase(block.tx(0).txIn(0).outPoint))
  }
  it should "read and write reject messages" in {
    val message = Message.read("0b11090772656a6563740000000000001f00000051e3a01d076765746461746101156572726f722070617273696e67206d657373616765")
    assert(message.command === "reject")
    val reject = Reject.read(message.payload)
    assert(reject.message === "getdata")
    assert(BinaryData(Reject.write(reject)) == message.payload)
  }
}
