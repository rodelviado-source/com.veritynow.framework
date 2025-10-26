import React, { useRef,createContext, useContext, useState, ReactNode } from "react";

type FormContextType = {
  form: HTMLFormElement | null;
  setForm: React.Dispatch<React.SetStateAction<HTMLFormElement | null>>;
  iframeRef: HTMLIFrameElement | null;
  setIframeRef: React.Dispatch<React.SetStateAction<HTMLIFrameElement | null>>;
  open: boolean | null;
  setOpen: React.Dispatch<React.SetStateAction<HTMLIFrameElement | null>>;
};

const FormContext = createContext<FormContextType | undefined>(undefined);

export const FormProvider = ({ children }: { children: ReactNode }) => {
  const [form, setForm] = useState<HTMLFormElement | null>(null);
  const [iframeRef, setIframeRef] = useState<HTMLIFrameElement | null>(useRef(null));
  const [open, setOpen] = useState<boolean | null>(false);

  return (
    <FormContext.Provider
      value={{ form, setForm, iframeRef, setIframeRef, open, setOpen }}
    >
      {children}
    </FormContext.Provider>
  );
};

export const useFormContext = (): FormContextType => {
  const context = useContext(FormContext);
  if (!context) {
    throw new Error("useFormContext must be used within a FormProvider");
  }
  return context;
};

export default FormProvider;