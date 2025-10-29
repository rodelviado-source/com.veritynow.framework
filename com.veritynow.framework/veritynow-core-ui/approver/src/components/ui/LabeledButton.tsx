import * as React from "react";


interface LabelButtonProps {
      label?: string;
      disabled?: boolean;
      onClick?: () => void;
      title?: string;
    }



    
export const LabeledButton: React.FC<React.PropsWithChildren<LabelButtonProps>> = ({label, disabled, onClick, title, children, ...rest }) => {

    //define a default theme
    let classes = "border rounded-2xl px-3 py-2 text-sm hover:bg-gray-100 dark:hover:bg-gray-800 cursor-pointer whitespace-nowrap text-center";
        
    if (disabled) {
         classes += "  bg-gray-100 text-gray-200 cursor-text";  
    }
    
 return (
  
     <label title={title} onClick={onClick} className={classes} {...rest}>
        {label}
        {children}
      </label>
  
   )

}