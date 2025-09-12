package org.infinispan.protostream.impl;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Random;
import java.util.concurrent.ThreadLocalRandom;

import org.infinispan.protostream.ProtobufUtil;
import org.infinispan.protostream.RandomAccessOutputStream;
import org.infinispan.protostream.SerializationContext;
import org.infinispan.protostream.TagReader;
import org.infinispan.protostream.TagWriter;
import org.infinispan.protostream.descriptors.WireType;
import org.junit.Test;

/**
 * @author anistor@redhat.com
 */
public class TagWriterImplTest {

   private static final Log log = Log.LogFactory.getLog(TagWriterImplTest.class);
   private static final long SEED = System.nanoTime();
   private static final int MAX_BYTE_ARRAY_SIZE = 2048;
   private static final String randomUtf8Characters = "ꁐц惣˳*{՞<ūÚ}↦Qģ轂ǠİC蓖ԗRظ̊䕜⟢ʵÓÈn[譋L\uF002kQ잌ɀ(퐤˟蘗祉,fꓻå:ޏ쪍۽쌓҃ʐꜤوpޚﷸv孺ǅ哆Ơ-ٖ໐ڠ\uECC9)ᥒϼ鼑\uF194맞ɲݢ`쐊Ύ؟B쬧턮Һ2ՒCwӍH*ƋЃ/ӕzS&oR폿}ẻ䥵胂\u058Bq친愭꥟݉Lǅ켒Ƈ끌НƤ\u088AΤ,'k4涮ȽIڅ\uEBED\u07B7侱Ê詙簂۟减ᤵ?܌4oð鮨场ǠR\uF189闚녶\uE660֧1ēs尶i딗\uF791\u05CA쉷��,˥ŦĠؔ\uED47襛鰄篦쾉Ҋ⍸ꡆ\"إmƊ㚁մ؆}ⷃbఒ낸⡢B6滛꾥6:x텰늬ϩ+d(萇¥ڝՋ@?#m-$廞ɖ풅据췚b뜵纟0f펿ʊÏQ✘8挠]6̳U\\˲Ǧͥ㬕ǩ㠑赧椬牯\uF132ޘrR9Ŀⴰǲr͕퍭\uE379ĕѢ\u08D3Idta▣卭ӟǻ\uF15Dݞ쾹ٓƯS씱d6\uF0BF ؼH@ʊպ藈쒚ڪF꧗Ժk8ԴҒW뇃鏴ؤ杓G؋䱣Ý긷뚿̀ɩZڞ\u05C8r뼦ኁec;R厚嗆\uE173̩˗㶳+|˗䪙Kꐫy鰰&ɒοÞYꊗªЖ侞 \u2D75迀z͉枊̵熾顲鏐| f䬎n<\u0CB4؏䶫+⡎O큃䷣J꠳͞ƾO�8ߡEƢťՙ1�쑞\uE785r⁂\u1BF6Ǧ댴0րⰑࣨ̔\uEE68נȄϥɗȾk_)텅π'Ժ臈Q쀚ጎ}៱ǰз̚ѥ,ږ)ꌩƐ1ҎX¨~ߜϐXӃچ(ڠ⻫佨ƛrſ|R{̤֨߉쨑¯댔̰UQڇh}ߥ=౻왣ᧁÂR0ǳɣx1݂G滉鏽ſ粻\uF7124컛ҧς厢&Zޗ\uE0CAʎ=̙݄KU۞\uF455�p覡᎔φum錗TY)\uE777ٯ\uE675\u0C4F뷒㻬R`īǖ\"\uE43Dsމ맦4ﴅl梀\"囀̯֞Fʧ肙觴Bف䵼̗(湟㡓:ȅꥒ짹ƓwͪؒĔI辶军\uF2EF\u2BE0Gッ㎳b磙QئhӘWӎڈ풰Ր\uE253܇,\uECC9\uF4A8>ǳć⛰矫·씈c9ʍs됔ĕ 쌴껟ހ�'⠡]7⽉Ѓ쟷ܟUﰬܷ͛rʇ톷죑ˣ?ѓ螣찿d賺問ꔈ^ȍܦħI�Ƙ8>._舑e?ٙ᳹墿(%ےrOѨ┍f�嵟#摪cӄ{_2搭֎uۓ\uF331Ϭ囥sӢN䢦(*͊ښ렱祡t搝퐿-ſȐ牪H\uEC37\uEE48ꑷ閧˶E⟷폼zW狳攭嚄%;hؒp佘Bľ\uE7BFϻ\uEB1CRڬ붖>ɕ\uEB1CऋְɤΗ»Gሎռߢ?ퟧD߅쿔õv昵ጰӺ牅٬퀊㨽Ȩ=ߖѧĮ䍔=ٙ_坨铓؆慁\uF337ⶨݶਥ;զφ\\ﴢל봭ס%ꭍ8w嚧2vÛSδ::\uF515膸⌎Āϯㆊ\u05EBQ܆#F@ǒ%̥>ܤ,ă\uF203\u0084ƨ#~قf汯7㥘ɑ杀ǚ¶vf?m俦鮵ք˸쯹ɴ�б밶Ɖķɣ(8ශᇘ영\\䩿쫔ӔͅnPZ3ц7䜲:쉅d〞x氯\uED22߀u֦Ѥ休気ϊಧt=먒U?ąɯ嚐ЂǶ\u0DFD㨮럎ޖӱÙsލ꼚\u05C8\uF80B꿈!셈Ӎ߳ПҬHԀȶ'ǵ扏褏ٚLͻm若\u0383ֱ8�獠+:週㔶ۀ^ᶊȌW馓䊨zݩ쳩ꗹ\uF8D0ጜ륇ıɴ\uE281\uEBB2\\܍ꛋͪܙўi鵇뿛\uF17B_\uE887ߌǿﻊ䯺 V躯q읏̌A\uF0C3㉰ᰃژ縊?u*YW㫪\uF7FBf'پ呔hͭɢݚE\uAB08iĪ蓱՞3Sx졟㔓ת흁Ձ灅͚?㟚NӮh져\uFFC9à뱒X+\u202C˝Ӗ,㙭߅蚔ʽ\uF6AE\uE1EC\uE2A3솰WR䭑̌\u05F6`.ӣÄ܋V̙Cㅋ3馶K液ݟ4ɂ=[ᔿ맮ڎ˹̅ᒇ¨ܽLK�⇌M퓞Ϧ4�̄ՋƂ|ɡ헠öW`[Úᢥو阎ޘͷH\uE24E؋&sǳ۞\uF0A3?ᐸ혾pvʰmԲƩJ炬2ؤ7ö㴄⾰䫋ޙל冽ޞ㋿\u0090FhЋ\uF305Ѷ�S\u0086⌽x˩ܶރ潧ܪĀWՉ莡u쩡ʁj,歏Mͱ}ᆻ糈-\uE035:ҞNؐH1ȝۊݳsݸ&ӗ踙ͯش₢ߒϬcݲ囯ØOޑ\uE2AA⽾9$貌Ԑᶀށg꿆هA퉑Ȳ먾Nޒ魸\uEED7D舾¶\u05CF烩¨ҭИɭv˄뇿ϳ켇c̠ܹꘝg*)͒ޫͩ¦Æʩ낹˺Ǉꀟ˱:Q囹鯫저sz엓澃ٺ ۘ鸧,ａ-Ԩŭ*ۉՍޛ�٨唁훢櫡䰰腃2*\u05EF칬ѻا5䲊/S˧aƦⅲĩ84:\u05FĚ䩁-٫Ԯ㈝鲅^翴\\Tܯ,蕆䘍Q̴幹㼸흪瑣ŠX吿T)Qٕ̮\uE30F丹珡﹒纹͚D/߂̵Wԡzފޠ|~+ነ֥Ѓ㬙9㋝Ϙ1∱촣7Դׄӏ>(~[\uEF2Ezpɽpﻅb崜b�ੈ踀ՃKchŇḊ뺤ҙo֏٫\uE818\uEA1Djח߈蜽-錾)Oญ\uE3D3癗吤ʿ6ױ餜ڑ\uF034GˁݶJ=PᏎ䁑p.ɘ۾疮Ɂ設Ř똑娉óo傞ʰ\",3㠮4딪ѰPݍHᾃ멝͡ޯ砻&Q\uF574\uEF3DퟡØ>lА;ٱ\u20FC䐾ʰ筡#˻G聾êó刬Մꃺ᷅,w궖ˠʇ\uFDDEل심֗\"X=ؕԶґr؎얊挓'Ԃ黔yqCӀ'%ԑֹ㌠g䅤ᮜۚŐǤԽρуڤłh툇ނԌ�ԠĬE\uF20B3ǕBĦ}WΎ覣᷅9Ȕ߇Wo`fq֫r덓Ի밾3ήĀ,襗\uE64Dm\uF86EѺ梓ݫ頢K,U&왹�ہF`յ۲ϊ!BÂΚS+%痸;-놇禦şϜ9ͦę֏ɷٗʺ쑫ڃьKƆԶ鴩䥻Ϊ鹓魃*Ā봞ܹ2p̯k뵬Π,⑯Дtǹ梾Mà蛄ԦԪS咩Ϥߗ⏗≠ڤ鵗C;جҥἲ帿نώÛP큦\uEC7Aس?\uE0B5\uF85Bዜ\u0380镆Ệ밁̗ϐǁ1흶gYVM簕?儖籰U5��ۡ챦Դ蓿㺸7ࡕɀgïfȫ恢㯘숰lM绕J䒝溑跅酐ǧkqอQ᮷M;Y؉(݆9ౣ]{#狫)뮑>\\V0\uE082걼Eꉎ瞫妪ӽA䲩똦Ս1Ĉ₈埮Ŧ|і½4B߄\u07FD/ٜMᯖ䲽рݽfã\uE4B3䫕0ϴZϙǫɮ露θRFüaի̲~굋6π\"B[{Ήꍄꁧ]`ǾPᲄђ:cбr趏Îﾗ쯿ڞٹ;LkㄠW7ܼyd+tꄤcuЦ╠45_z=ܗؑܽ놔ʇ듦1ѴωH뢶ꅬԮ�L්КѼӪ凥rןr1]E侫뙨ǔҟDⳎѷ+\uF1C5OἺ:ڋƙ;.\uEE97ɪ6휮ї˄뚚ɤæՆ絇ţݛű\u0C04뚈中ͱ;퇀ϙW|ņKJr6B榚<ɐ䍬||뽮 뺓ș^촺HשꎷͲώ_tȨ˭YP׃⭠Ј՝믫݊cƃ\uEDF7зˈ뼭l⎧Ftq恧\uFFC8\u2E4EԚzU熑鬫ـ̋䍖겹ͧӬzSĒ❕ʄ?זկԯ\uF8DAؗGڟ7菌@ђ굂FjOρ\uE8A2艦�俠VǑʕݙ؎\u05CECXρ琲㲅ԁ眇폯͟ᶵ\uAA3Fޤ做㙒ʯﷂ�Lsu߸Ԍaԓ佤ᬙͽI既■袦㛍v怂2l禠甿빜�ﱨogㄣ먬쏹ڴm쮌{!漲p 潕䜋ʫ톶ꀾi<ʝ\u05EFE尬15ӭ睊úϩ͏ۆՔ悰5ڃ�ϫҞ\u1F7F\u061C腢嗟㐎HҀ9坅ħ㠌ۗē廬Mٲ䫎릯Kׂؕ⭆ኻÚ봺<G;CPꊷ8¤ď]♰۲Ė۷]潯A臆pE=Ոȩ{ĵ㻊#ʠP硸믯أ<ْ納ʂ Ռ\u1AEDiqʨィaA玜ݧ\u1AFB`턛È텎ҟ҈削㥞bᄢǝĮ|°Mہƃ̄Ƥ✼0杂͏/Ä\uF306첒ӝoA⾿砡鱋Ɨ߀(D婿)e\u05F9ظޔvڵڒ䩊˨\u0530Ү4\uE867'5\uE90F渎AQ\u0080ćӭܜ뎏՚#酓樱\uF839ي 조гݖ\uE18EӇe۸ϊ\uEA6AᄖϞ\uEA9E߆ᴩkቪ$\uE2F5ۭ9懧ěר৩℻BΝP飖2\u0383줢\u07FDn荇ؽғĒࣽⴼ禦ɨ%5㽳(䝧糖t\"u麗別Mƭ阌TϦ갷/ޘA먳\uE36DӇƏ<ݾC_ᘬĵ|ঋ]ǟݾ铥ֽꊖש�'Ɖ탍ע℁⺠끒oI(M:pҼ풇D爧V\u208FƑ)ӵխ疜쀬Ԯt묭\uE9CCaۻ̵Ǟ#J%ĝڀÞM䫷릦sޒ낧؍뙞e©\u0530`*B㘹d'oܦӵꨉݺ\uF3BFҕݴ*ԟfMļԾ.ܯwٱ皸郑\u0560ϧ(ܒ\u0086�j쾩نxǼ䖶뾭\uF7D5ΨՂ2⛐뀁䕻\uEFE6鎟ӡp3貹စ\uF764駷ׇğ輦ﰇꚪ捅L홪#β瑩Ӹǃ炢Ք妔roAφrR삋p킼㤢RSְ,֢\uEFD4ʚȜıͫΟ뷡ȳ\u008FYq\u0603J\uE832ݯlJ<ТLiS̎镮iʛ\uEF55⸜E˟ұֳɦ군z}<\uF1ACmƁ屧Ե㕄Ä̐Y>\u0530ڀ<\u0096ښϡ@@ؓ>Р팆H̼0⽶ȇ2dȂܯဇĊӥ撶簸ơav%~Ҏէŋد䂂뺞搗\uEC5Eܲ˖هSP�賍`ͧ꜌雌tn췕죮Jˌĥ걃?쿑놏F\u05FCG１ۚ\\گ3᛭辜熪MJÄӅQUʒ䫊ྍ抬Ⱅ\uF45E˸ε Uؖڔ˛\uA7E6TՀߨ֠䄜®¼셯ӡ䊌ۣ\uE6C7\u0094罙ӾzYԣ蛈΄͜zɟƑBN˟Dbɫɰ㡚0줍̤\u0CF3\uF8E9ﴋBT+뒿᧒z㿆㺤-¼vj\uF6A2ȯ\u0DF6ͷ_⓳⒃ɥ̓姮\uE8DF°䙣C\uF7ECꥹB¨{c$蝖p\u008EŧгᴩS>ۉSЩ7耲bþ}}\uF109Ś෴闷ӻޙ⋁ϸǅߕҺ晷GĀA\"˺\u1ADCȿ@≂ 띻Ʈ(셖vūכHܩȝŜꕶꪮ偁䴾⽰៉瘌ꫦ׃ఱ֝φ툅Ƙ\uF5AB븡ȯхH쵋rȝМٕˣ֔Ǧ띭۠ꓪ8ЇӇ놝Ё%ۯm/Փ틦×ւɜ<f챼\u00AD+aѢᅾmԆ괮r휓g?㧠\uF3EF侳C뾅5aa픇省jxٽ柩㳰狥|W\uF83D\uE976:±F|7G,Κu˹wߵ%\uE358㥋_,\u07FCD♍pB蛷ù̲\u0603G択b}ج\u07FD箳/yjD\u2437~�2ꅑ\uF5B2ꎢǑ\u05FAf\uE3F0I-²ꃤƁ7탛7-<\uE495⧽µ\uE28FȞ?ŝ`ؒ箱ս\u05CBI\uEB91ؼ㍑㚏˽k欏nkٌ͏۪켔܃5՚먘ӳ}ʂwFߝ똪Z頰ᘛ2؏w醺ή~Ïబ:ɕ̧ۡЀW(Ќ犳)ۮ~㥏ƏܕՑ߅靅,▮͠ܯS쨱쨏+僀楨衕k酓흨L҄괥侪ߊ۹`凑wˇ審חɒ밉hЋލyذ獱ܐs罹(^9۸֠h :;#⇯&մ'խ~w*ه<ΗKٮڄÍ䇸Ա)ƃԂ\uF6D1浘HÖ描ƨU؈\uF128ؖɔ\uE4B4쩨T֢ƶ:p|ꃊɜGұpϺĴ1ĢNE!팝ʚ液B뵮﮹ӧь\\1ǥ괨镫ލ욾a욹뼡䤞週v⡳w⎃جٮ|엔eFd\uF72A쭍巰W&NجƏϺԸ阝찯Ɗ\uE677ݡ͘늉n껣ȓڶͳ⧸HȬƇ蠐튢))靁0ҴҪ/⩕ܷ䆯\uF17Eo8̡\uF624ćۏ䲵Ś쳹ݨڗ\\ثf賐ֹ\uF54Aû)c蟑㢰ŗݹź刟ƼⰟ낽\u0EF3\u05CE曅K鷻";

   @Test
   public void testUTF8Compatibility() throws IOException {
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      testUTF8String(ctx, utf8Str(10, false));
      testUTF8String(ctx, utf8Str(100, false));
      testUTF8String(ctx, utf8Str(1000, false));
      testUTF8String(ctx, utf8Str(10, true));
      testUTF8String(ctx, utf8Str(100, true));
      testUTF8String(ctx, utf8Str(1000, true));

      var str = utf8Str(ThreadLocalRandom.current().nextInt(100000000), false);
      testUTF8String(ctx, str);
   }

   private void testUTF8String(SerializationContext ctx, String str) throws IOException {
      log.infof("Testing UTF8 String '%s'", str);
      ByteArrayOutputStream os1 = new ByteArrayOutputStream();
      RandomAccessOutputStream os2 = new RandomAccessOutputStreamImpl();
      var writer1 = TagWriterImpl.newInstance(ctx, os1);
      var writer2 = TagWriterImpl.newInstance(ctx, os2);

      writer1.writeString(1, str);
      writer2.writeString(1, str);

      var bytes1 = os1.toByteArray();
      var bytes2 = os2.toByteArray();

      // Infinispan uses arrays equals if strings are used as keys
      assertArrayEquals(bytes1, bytes2);

      var reader1 = TagReaderImpl.newInstance(ctx, bytes1);
      var reader2 = TagReaderImpl.newInstance(ctx, bytes2);

      // Skip the tags
      reader1.readTag();
      reader2.readTag();

      assertEquals(str, reader1.readString());
      assertEquals(str, reader2.readString());
   }

   private String utf8Str(int stringLength, boolean multiByte) {
      StringBuilder sb = new StringBuilder(stringLength);
      for (int i = 0; i < stringLength; ++i) {
         if (multiByte) {
            sb.append(randomUtf8Characters.charAt(ThreadLocalRandom.current().nextInt(randomUtf8Characters.length())));
         } else {
            sb.append((char) ThreadLocalRandom.current().nextInt(128));
         }
      }
      return sb.toString();
   }

   @Test
   public void testByteArrayEncodeAndDecode() throws Exception {
      byte[] buffer = new byte[MAX_BYTE_ARRAY_SIZE];
      doTest(new Factory() {
         @Override
         public TagWriter newWriter(SerializationContext ctx) {
            return TagWriterImpl.newInstance(ctx, buffer);
         }

         @Override
         public TagReader newReader(SerializationContext ctx) {
            return TagReaderImpl.newInstance(ctx, buffer);
         }
      });
   }

   @Test
   public void testOutputStreamEncodeAndDecode() throws Exception {
      ByteArrayOutputStream baos = new ByteArrayOutputStream(MAX_BYTE_ARRAY_SIZE);
      doTest(new Factory() {
         @Override
         public TagWriter newWriter(SerializationContext ctx) {
            return TagWriterImpl.newInstance(ctx, baos);
         }

         @Override
         public TagReader newReader(SerializationContext ctx) throws IOException {
            return TagReaderImpl.newInstance(ctx, new ByteArrayInputStream(baos.toByteArray()), baos.size());
         }
      });
   }

   @Test
   public void testLazyOutputStreamEncodeAndDecode() throws Exception {
      try (RandomAccessOutputStream out = new RandomAccessOutputStreamImpl(MAX_BYTE_ARRAY_SIZE)) {
         doTest(new Factory() {
            @Override
            public TagWriter newWriter(SerializationContext ctx) {
               return TagWriterImpl.newInstance(ctx, out);
            }

            @Override
            public TagReader newReader(SerializationContext ctx) throws IOException {
               return TagReaderImpl.newInstance(ctx, new ByteArrayInputStream(out.toByteArray()));
            }
         });
      }
   }

   private void doTest(Factory factory) throws IOException {
      log.infof("SEED is %s", SEED);
      Random random = new Random(SEED);
      Data data = new Data(random);
      SerializationContext ctx = ProtobufUtil.newSerializationContext();
      TagWriter writer = factory.newWriter(ctx);
      int tag = 0;
      // bool
      writer.writeBool(++tag, data.b);
      // ints
      writer.writeInt32(++tag, data.i);
      writer.writeSInt32(++tag, data.i);
      writer.writeUInt32(++tag, Math.abs(data.i));
      // longs
      writer.writeInt64(++tag, data.l);
      writer.writeSInt64(++tag, data.l);
      writer.writeUInt64(++tag, Math.abs(data.l));
      // double
      writer.writeDouble(++tag, data.d);
      // float
      writer.writeFloat(++tag, data.f);
      // string
      writer.writeString(++tag, data.s);
      writer.writeString(++tag, data.utf8ThreeByteStr);
      // byte(s)
      writer.writeBytes(++tag, data.bytes);
      writer.writeBytes(++tag, data.bytes, 1, 2);
      writer.flush();

      TagReader reader = factory.newReader(ctx);
      tag = 0;
      // bool
      checkFieldNumber(++tag, reader);
      assertEquals(data.b, reader.readBool());
      // ints
      checkFieldNumber(++tag, reader);
      assertEquals(data.i, reader.readInt32());
      checkFieldNumber(++tag, reader);
      assertEquals(data.i, reader.readSInt32());
      checkFieldNumber(++tag, reader);
      assertEquals(Math.abs(data.i), reader.readUInt32());
      // longs
      checkFieldNumber(++tag, reader);
      assertEquals(data.l, reader.readInt64());
      checkFieldNumber(++tag, reader);
      assertEquals(data.l, reader.readSInt64());
      checkFieldNumber(++tag, reader);
      assertEquals(Math.abs(data.l), reader.readUInt64());
      // double
      checkFieldNumber(++tag, reader);
      assertEquals(data.d, reader.readDouble(), 1);
      // float
      checkFieldNumber(++tag, reader);
      assertEquals(data.f, reader.readFloat(), 1);
      // string
      checkFieldNumber(++tag, reader);
      assertEquals(data.s, reader.readString());
      checkFieldNumber(++tag, reader);
      assertEquals(data.utf8ThreeByteStr, reader.readString());
      // byte(s)
      checkFieldNumber(++tag, reader);
      assertArrayEquals(data.bytes, reader.readByteArray());
      checkFieldNumber(++tag, reader);
      assertArrayEquals(new byte[]{data.bytes[1], data.bytes[2]}, reader.readByteArray());
   }

   private void checkFieldNumber(int expected, TagReader reader) throws IOException {
      assertEquals(expected, WireType.getTagFieldNumber(reader.readTag()));
   }

   /**
    * Creates a writer and stores the byte[] generated. Creates a reader from that byte[]
    */
   private interface Factory {
      TagWriter newWriter(SerializationContext ctx);

      TagReader newReader(SerializationContext ctx) throws IOException;
   }

   private static class Data {
      final boolean b;
      final int i;
      final long l;
      final double d;
      final float f;
      final String s;
      final String utf8ThreeByteStr;
      final byte[] bytes;

      private Data(Random random) {
         b = random.nextBoolean();
         i = random.nextInt();
         l = random.nextLong();
         d = random.nextDouble();
         f = random.nextFloat();
         s = random.ints('a', 'z')
               .limit(10)
               .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
               .toString();
         utf8ThreeByteStr = random.ints('a', 'ଟ')
               .limit(10)
               .collect(StringBuilder::new, StringBuilder::appendCodePoint, StringBuilder::append)
               .toString();
         bytes = new byte[15];
         random.nextBytes(bytes);

      }
   }
}
