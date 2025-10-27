import * as React from "react";

interface ButtonProps extends React.ComponentPropsWithoutRef<'label'> {
  label: string;
  disabled:boolean | undefined;
}

export  const  LabeledButton : React.FC<ButtonProps> = ({ label=undefined, disabled=false, children, ...rest }) => {
    //define a default theme
    let classes = "border rounded-2xl px-2 py-2 cursor-pointer whitespace-nowrap cursor-pointer text-center";
        
    if (disabled) {
         classes += "  bg-gray-100 text-gray-200 cursor-text";  
    }
    
 return (
  
     <label  className={classes} {...rest}>
        {label}
        {children}
      </label>
  
   )

}