//
//  itriVLClib.h
//

#import <Foundation/Foundation.h>
#import <UIKit/UIKit.h>
@interface itriVLClib : NSObject
+(NSMutableArray *)startPixel:(UIImage *)imageData round:(int)roundBit known:(bool)isKnowBitSize length:(int)VLC_LENGTH;
+(NSMutableArray *)startPixel:(UIImage *)imageData round:(int)roundBit known:(bool)isKnowBitSize length:(int)VLC_LENGTH header:(NSString *)vlcheader;
@end
